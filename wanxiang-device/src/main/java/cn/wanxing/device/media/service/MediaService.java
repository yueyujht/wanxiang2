package cn.wanxing.device.media.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.common.log.ApiLog;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.media.dto.MediaQueryRequest;
import cn.wanxing.device.media.entity.MediaFile;
import cn.wanxing.device.media.mapper.MediaFileMapper;
import cn.wanxing.device.media.message.MediaUploadMessage;
import cn.wanxing.device.mqtt.DeviceTopicConst;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.device.wayline.service.WaylineJobService;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 媒体服务：处理媒体文件上传结果（events: file_upload_callback）入库并回执，
 * 响应设备的高优先级媒体查询，提供媒体列表与删除。
 *
 * <p>文件本体由设备直传对象存储（凭证经 requests: storage_config_get 下发，见 BindingService），
 * 平台只维护元数据索引；对象存储中文件本体的删除需要 OSS SDK，当前仅删索引记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class MediaService {

    /** 需要回执的标记值（events 消息 need_reply 字段） */
    private static final int NEED_REPLY = 1;

    /** 媒体 WebSocket 推送主题前缀（前端订阅 /topic/device/{sn}/media 实时刷新列表） */
    private static final String MEDIA_TOPIC_PREFIX = "/topic/device/";

    private final ObjectMapper objectMapper;

    private final MediaFileMapper mediaFileMapper;

    private final DeviceMapper deviceMapper;

    private final MqttPublisher mqttPublisher;

    private final WaylineJobService waylineJobService;

    private final UserContext userContext;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 处理一条媒体上传结果消息：按 object_key 去重入库，推送前端，need_reply=1 时回执
     *
     * @param sn      主题中的网关设备序列号（机场）
     * @param payload 消息原文（JSON 字符串）
     */
    public void handleUploadCallback(String sn, String payload) {
        // 1.读取消息
        MediaUploadMessage message;
        try {
            message = objectMapper.readValue(payload, MediaUploadMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("解析媒体上传结果消息失败 sn={} payload={}", sn, payload, e);
            return;
        }
        if (message.getData() == null || message.getData().getFile() == null) {
            return;
        }

        // 2.按 object_key 去重入库（设备重发/重传场景不产生重复记录）
        MediaFile media = MediaFile.create(sn, message.getData().getFile(), message.getData().getFlightTask());
        MediaFile existing = mediaFileMapper.selectOne(
                new LambdaQueryWrapper<MediaFile>().eq(MediaFile::getObjectKey, media.getObjectKey()));
        if (existing == null) {
            Assert.isTrue(mediaFileMapper.insert(media) > 0, () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));
        } else {
            media.setId(existing.getId());
            log.info("媒体文件记录已存在，跳过入库 object_key={}", media.getObjectKey());
        }

        // 3.实时推送给订阅该设备的前端
        pushMediaFile(sn, media);

        // 4.设备要求回执时回复 events_reply（result=0 表示已处理，设备不再重发）
        if (Integer.valueOf(NEED_REPLY).equals(message.getNeedReply())) {
            sendEventsReply(sn, message.getTid(), message.getBid(), message.getMethod(), null);
        }
    }

    /**
     * 响应设备的高优先级媒体查询（events: highest_priority_upload_flighttask_media）：
     * 返回该设备当前进行中任务（sent/executing）的 flight_id——任务媒体优先上传；
     * 无进行中任务时仅回执确认，设备按默认顺序上传
     */
    public void handlePriorityQuery(String sn, String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析媒体优先级查询消息失败 sn={} payload={}", sn, payload, e);
            return;
        }
        String flightId = waylineJobService.findActiveFlightId(sn);
        if (flightId != null) {
            log.info("收到媒体高优先级查询，回进行中任务 sn={} flightId={}", sn, flightId);
        } else {
            log.info("收到媒体高优先级查询，无进行中任务，按默认顺序上传 sn={}", sn);
        }
        ObjectNode output = null;
        if (flightId != null) {
            output = objectMapper.createObjectNode();
            output.put("flight_id", flightId);
        }
        sendEventsReply(sn, root.path("tid").asText(null), root.path("bid").asText(null),
                root.path("method").asText(null), output);
    }

    /**
     * 媒体列表：分页 + 按文件名/任务筛选。
     * 机构隔离：机构用户只能看到本机构设备的媒体（子查询实时关联 sys_device，
     * 不引入冗余字段也没有缓存一致性问题）；平台超管可看全部
     */
    @ApiLog("媒体列表")
    public MultiResult<MediaFile> listFiles(MediaQueryRequest req) {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<MediaFile> qw = new LambdaQueryWrapper<>();
        if (operator.getOrgId() != null) {
            qw.apply("device_sn IN (SELECT sn FROM sys_device WHERE org_id = {0})", operator.getOrgId());
        }
        qw.like(req.getFileName() != null && !req.getFileName().isBlank(), MediaFile::getFileName, req.getFileName());
        qw.eq(req.getFlightId() != null && !req.getFlightId().isBlank(), MediaFile::getFlightId, req.getFlightId());
        qw.orderByDesc(MediaFile::getCreatedAt);

        Page<MediaFile> page = mediaFileMapper.selectPage(new Page<>(req.getCurrentPage(), req.getPageSize()), qw);
        return MultiResult.successMulti(page.getRecords(), page.getTotal(), req.getCurrentPage(), req.getPageSize());
    }

    /**
     * 删除媒体记录：校验机构归属后删索引记录（对象存储中的文件本体删除需 OSS SDK，暂不处理）
     */
    @ApiLog("删除媒体文件")
    public Boolean delete(Long id) {
        // 1.查询媒体记录
        MediaFile media = mediaFileMapper.selectById(id);
        if (media == null) {
            throw new DeviceException(DeviceErrorCode.MEDIA_FILE_NOT_FOUND);
        }

        // 2.机构隔离：机构用户只能删本机构设备的媒体
        User operator = userContext.currentUser();
        if (operator.getOrgId() != null) {
            Device device = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>().eq(Device::getSn, media.getDeviceSn()));
            if (device == null || !Objects.equals(operator.getOrgId(), device.getOrgId())) {
                throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
            }
        }

        // 3.删除索引记录
        Assert.isTrue(mediaFileMapper.deleteById(id) > 0, () -> new DeviceException(DeviceErrorCode.UPDATE_FAILED));
        return Boolean.TRUE;
    }

    /**
     * 媒体文件入库结果推送给前端：{file 元数据}
     */
    private void pushMediaFile(String sn, MediaFile media) {
        try {
            messagingTemplate.convertAndSend(MEDIA_TOPIC_PREFIX + sn + "/media",
                    objectMapper.writeValueAsString(media));
        } catch (JsonProcessingException e) {
            log.warn("媒体推送序列化失败 sn={}", sn, e);
        }
    }

    /**
     * 回复事件回执到 events_reply：回传相同的 tid/bid/method（设备靠它们匹配事件与应答）；
     * output 非空时附带业务输出（如媒体优先级查询的 flight_id）
     */
    private void sendEventsReply(String sn, String tid, String bid, String method, JsonNode output) {
        ObjectNode reply = objectMapper.createObjectNode();
        reply.put("tid", tid);
        reply.put("bid", bid);
        reply.put("timestamp", System.currentTimeMillis());
        reply.put("method", method);
        ObjectNode data = reply.putObject("data");
        data.put("result", 0);
        if (output != null) {
            data.set("output", output);
        }

        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn
                + DeviceTopicConst.EVENTS_SUF + DeviceTopicConst.REPLY_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(reply), "回复媒体上传结果");
        } catch (JsonProcessingException e) {
            log.error("序列化 events_reply 失败 sn={}", sn, e);
        }
    }
}
