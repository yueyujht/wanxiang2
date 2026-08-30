package cn.wanxing.device.flightarea.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.common.log.ApiLog;
import cn.wanxing.common.log.TraceContext;
import cn.wanxing.device.config.FlightAreaProperties;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.flightarea.dto.FlightAreaFileRequest;
import cn.wanxing.device.flightarea.entity.FlightAreaFile;
import cn.wanxing.device.flightarea.mapper.FlightAreaFileMapper;
import cn.wanxing.device.flightarea.message.FlightAreasDroneLocationMessage;
import cn.wanxing.device.flightarea.message.FlightAreasSyncProgressMessage;
import cn.wanxing.device.mqtt.DeviceTopicConst;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 自定义飞行区服务：飞行区文件管理（官方 JSON 格式，存平台库）+ 同步链路。
 *
 * <p>链路：平台 HTTP 创建文件 → 下发 flight_areas_update 通知设备 → 设备经 requests: flight_areas_get
 * 拉取文件列表（下载 URL 指向平台接口，内网场景不依赖对象存储）→ 设备回报同步进度
 * （events: flight_areas_sync_progress）→ 飞行中回报区域告警（events: flight_areas_drone_location）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class FlightAreaService {

    /** 需要回执的标记值（events 消息 need_reply 字段） */
    private static final int NEED_REPLY = 1;

    /** WebSocket 推送主题：同步进度与指令回执 / 告警位置 */
    private static final String SYNC_TOPIC_PREFIX = "/topic/device/";
    private static final String SYNC_TOPIC_SUFFIX = "/flight-area/sync";
    private static final String WARNING_TOPIC_SUFFIX = "/flight-area/warning";

    private final ObjectMapper objectMapper;

    private final FlightAreaFileMapper flightAreaFileMapper;

    private final DeviceMapper deviceMapper;

    private final MqttPublisher mqttPublisher;

    private final UserContext userContext;

    private final FlightAreaProperties flightAreaProperties;

    private final SimpMessagingTemplate messagingTemplate;

    // ============ 文件管理（HTTP） ============

    /**
     * 创建飞行区文件：校验 JSON 合法性，计算 SHA256 签名与大小；
     * 机构用户创建的文件挂本机构（设备拉取时按机构过滤），平台超管创建的为全局文件
     */
    @ApiLog("创建飞行区文件")
    public FlightAreaFile createFile(FlightAreaFileRequest req) {
        // 1.校验内容为合法 JSON 对象（完整模板结构校验由前端规划工具保证）
        JsonNode content;
        try {
            content = objectMapper.readTree(req.getContent());
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.PROPERTY_VALUE_INVALID);
        }
        Assert.isTrue(content.isObject(), () -> new DeviceException(DeviceErrorCode.PROPERTY_VALUE_INVALID));

        // 2.归属机构：超管（无机构）创建的为全局文件
        User operator = userContext.currentUser();
        FlightAreaFile file = FlightAreaFile.create(req.getName(), req.getContent());
        file.setOrgId(operator.getOrgId());
        Assert.isTrue(flightAreaFileMapper.insert(file) > 0, () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));
        return file;
    }

    /**
     * 飞行区文件列表：机构用户可见本机构 + 全局文件，平台超管可见全部
     */
    @ApiLog("飞行区文件列表")
    public List<FlightAreaFile> listFiles() {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<FlightAreaFile> qw = new LambdaQueryWrapper<>();
        if (operator.getOrgId() != null) {
            qw.and(w -> w.isNull(FlightAreaFile::getOrgId).or().eq(FlightAreaFile::getOrgId, operator.getOrgId()));
        }
        return flightAreaFileMapper.selectList(qw);
    }

    /**
     * 删除飞行区文件：机构用户只能删本机构文件（全局文件仅超管可删）
     */
    @ApiLog("删除飞行区文件")
    public Boolean deleteFile(Long id) {
        FlightAreaFile file = getFileWithAccessCheck(id);
        Assert.isTrue(flightAreaFileMapper.deleteById(file.getId()) > 0,
                () -> new DeviceException(DeviceErrorCode.UPDATE_FAILED));
        return Boolean.TRUE;
    }

    /**
     * 设备下载飞行区文件（SaToken 放行，调用方是机场）：不存在时 404 语义由异常处理转错误响应
     */
    public FlightAreaFile getFileForDownload(Long id) {
        return flightAreaFileMapper.selectById(id);
    }

    // ============ 同步链路（MQTT） ============

    /**
     * 下发同步指令（services: flight_areas_update，data=null）：
     * 通知设备云端飞行区有更新，设备随即经 flight_areas_get 拉取文件列表
     */
    @ApiLog("下发飞行区同步")
    public Boolean sync(String sn) {
        // 1.校验设备合法
        checkAccess(sn);

        // 2.组装指令（协议规定 data=null）
        ObjectNode message = objectMapper.createObjectNode();
        message.put("tid", TraceContext.traceIdOrNew());
        message.put("bid", UUID.randomUUID().toString());
        message.put("timestamp", System.currentTimeMillis());
        message.put("method", "flight_areas_update");
        message.putNull("data");

        // 3.下发
        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn + DeviceTopicConst.SERVICES_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(message), "下发飞行区同步");
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.FLIGHT_AREA_SYNC_FAILED);
        }
        return Boolean.TRUE;
    }

    /**
     * 组装 flight_areas_get 的文件列表（requests: 自定义飞行区文件获取）：
     * 按网关设备的机构过滤（全局文件对全部机构可见），没有则为空数组；
     * 下载 URL 由平台提供（文件存库，不经对象存储），地址必须是机场可达的平台地址
     */
    public ObjectNode buildFilesOutput(String sn) {
        ArrayNode files = objectMapper.createArrayNode();
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        Long orgId = device == null ? null : device.getOrgId();
        if (device == null) {
            log.warn("收到未建档设备的飞行区文件获取请求，返回空列表 sn={}", sn);
        }
        LambdaQueryWrapper<FlightAreaFile> qw = new LambdaQueryWrapper<>();
        if (orgId != null) {
            qw.and(w -> w.isNull(FlightAreaFile::getOrgId).or().eq(FlightAreaFile::getOrgId, orgId));
        }
        for (FlightAreaFile file : flightAreaFileMapper.selectList(qw)) {
            ObjectNode item = files.addObject();
            item.put("name", file.getName());
            item.put("url", buildDownloadUrl(file.getId()));
            item.put("checksum", file.getChecksum());
            item.put("size", file.getSize());
        }
        ObjectNode output = objectMapper.createObjectNode();
        output.set("files", files);
        return output;
    }

    /**
     * 处理同步进度回执（services_reply: flight_areas_update）：记日志并推送前端
     */
    public void handleUpdateReply(String sn, String payload) {
        int result = parseResult(payload, sn);
        log.info("飞行区同步指令回执 sn={} result={}", sn, result);
        pushSync(sn, "update_reply", result, null, null);
    }

    /**
     * 处理同步进度事件（events: flight_areas_sync_progress）：
     * 状态实时推送前端，need_reply=1 时回执（不回执设备会重发）
     */
    public void handleSyncProgress(String sn, String payload) {
        FlightAreasSyncProgressMessage message = readMessage(payload, sn, FlightAreasSyncProgressMessage.class);
        if (message == null || message.getData() == null) {
            return;
        }
        FlightAreasSyncProgressMessage.SyncProgressData data = message.getData();
        log.info("飞行区同步进度 sn={} status={} reason={} file={}",
                sn, data.getStatus(), data.getReason(),
                data.getFile() == null ? null : data.getFile().getName());
        pushSync(sn, data.getStatus(), data.getReason(),
                data.getFile() == null ? null : data.getFile().getName(),
                data.getFile() == null ? null : data.getFile().getChecksum());
        if (Integer.valueOf(NEED_REPLY).equals(message.getNeedReply())) {
            sendEventsReply(sn, message.getTid(), message.getBid(), message.getMethod());
        }
    }

    /**
     * 处理飞行区告警事件（events: flight_areas_drone_location，need_reply=0 不回执）：
     * 飞行器与各区域的距离/进出状态实时推送前端
     */
    public void handleDroneLocation(String sn, String payload) {
        FlightAreasDroneLocationMessage message = readMessage(payload, sn, FlightAreasDroneLocationMessage.class);
        if (message == null || message.getData() == null || message.getData().getDroneLocations() == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSend(SYNC_TOPIC_PREFIX + sn + WARNING_TOPIC_SUFFIX,
                    objectMapper.writeValueAsString(message.getData().getDroneLocations()));
        } catch (JsonProcessingException e) {
            log.warn("飞行区告警推送序列化失败 sn={}", sn, e);
        }
    }

    // ============ 内部辅助 ============

    /**
     * 下载 URL：配置的平台基地址 + 下载接口路径；基地址未配置时返回空串并告警
     * （设备拿到无效地址会同步失败，日志留证据）
     */
    private String buildDownloadUrl(Long fileId) {
        String baseUrl = flightAreaProperties.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            log.error("飞行区下载基地址未配置（wanxiang.flight-area.base-url），设备将无法下载飞行区文件");
            return "";
        }
        return baseUrl + "/device/flight-area/files/" + fileId + "/download";
    }

    private void checkAccess(String sn) {
        User operator = userContext.currentUser();
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (device == null) {
            throw new DeviceException(DeviceErrorCode.DEVICE_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), device.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
    }

    /**
     * 机构归属校验：全局文件（org_id 为 NULL）仅平台超管可删
     */
    private FlightAreaFile getFileWithAccessCheck(Long id) {
        User operator = userContext.currentUser();
        FlightAreaFile file = flightAreaFileMapper.selectById(id);
        if (file == null) {
            throw new DeviceException(DeviceErrorCode.FLIGHT_AREA_FILE_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), file.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        return file;
    }

    private int parseResult(String payload, String sn) {
        try {
            return objectMapper.readTree(payload).path("data").path("result").asInt(-1);
        } catch (JsonProcessingException e) {
            log.warn("解析飞行区回执失败 sn={} payload={}", sn, payload, e);
            return -1;
        }
    }

    private <T> T readMessage(String payload, String sn, Class<T> clazz) {
        try {
            return objectMapper.readValue(payload, clazz);
        } catch (JsonProcessingException e) {
            log.warn("解析飞行区事件消息失败 sn={} payload={}", sn, payload, e);
            return null;
        }
    }

    private void pushSync(String sn, String status, Integer reason, String fileName, String checksum) {
        ObjectNode push = objectMapper.createObjectNode();
        push.put("type", status);
        push.put("result", reason);
        if (fileName != null) {
            push.put("fileName", fileName);
        }
        if (checksum != null) {
            push.put("checksum", checksum);
        }
        try {
            messagingTemplate.convertAndSend(SYNC_TOPIC_PREFIX + sn + SYNC_TOPIC_SUFFIX,
                    objectMapper.writeValueAsString(push));
        } catch (JsonProcessingException e) {
            log.warn("飞行区同步推送序列化失败 sn={}", sn, e);
        }
    }

    /**
     * 回复事件回执到 events_reply：回传相同的 tid/bid/method（设备靠它们匹配事件与应答）
     */
    private void sendEventsReply(String sn, String tid, String bid, String method) {
        ObjectNode reply = objectMapper.createObjectNode();
        reply.put("tid", tid);
        reply.put("bid", bid);
        reply.put("timestamp", System.currentTimeMillis());
        reply.put("method", method);
        reply.putObject("data").put("result", 0);

        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn
                + DeviceTopicConst.EVENTS_SUF + DeviceTopicConst.REPLY_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(reply), "回复飞行区事件");
        } catch (JsonProcessingException e) {
            log.error("序列化 events_reply 失败 sn={}", sn, e);
        }
    }
}
