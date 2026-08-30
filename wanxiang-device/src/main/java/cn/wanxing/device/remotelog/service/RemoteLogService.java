package cn.wanxing.device.remotelog.service;

import cn.wanxing.common.log.ApiLog;
import cn.wanxing.common.log.TraceContext;
import cn.wanxing.device.config.OssProperties;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.device.remotelog.dto.RemoteLogListRequest;
import cn.wanxing.device.remotelog.dto.RemoteLogUploadRequest;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * 远程日志服务：下发 fileupload_list / start / update，接收回执与 fileupload_progress 进度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class RemoteLogService {

    private static final String REMOTE_LOG_TOPIC_PREFIX = "/topic/device/";

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    private final OssProperties ossProperties;

    private final DeviceMapper deviceMapper;

    private final UserContext userContext;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 查询设备可上传日志文件列表
     */
    @ApiLog("查询设备日志文件列表")
    public void listLogs(String sn, RemoteLogListRequest req) {
        // 1.校验设备合法
        checkAccess(sn);

        // 2.构建message
        ObjectNode message = baseMessage("fileupload_list");
        ObjectNode data = objectMapper.createObjectNode();

        ArrayNode modules = data.putArray("module_list");
        if (req.getModuleList() == null || req.getModuleList().isEmpty()) {
            modules.add("0");
            modules.add("3");
        } else {
            req.getModuleList().forEach(modules::add);
        }
        message.set("data", data);

        // 3.发布websocke
        publish(sn, message);
    }

    /**
     * 发起日志文件上传（凭证来自 wanxiang.oss 配置）
     */
    @ApiLog("上传日志")
    public void startUpload(String sn, RemoteLogUploadRequest req) {
        checkAccess(sn);
        ObjectNode message = baseMessage("fileupload_start");
        ObjectNode data = objectMapper.createObjectNode();
        data.put("bucket", ossProperties.getBucket());
        data.put("region", ossProperties.getRegion());
        data.put("endpoint", ossProperties.getEndpoint());
        data.put("provider", ossProperties.getProvider());
        ObjectNode credentials = data.putObject("credentials");
        credentials.put("access_key_id", ossProperties.getAccessKeyId());
        credentials.put("access_key_secret", ossProperties.getAccessKeySecret());
        credentials.put("expire", System.currentTimeMillis() + 3600000L);
        credentials.put("security_token", ossProperties.getSecurityToken());
        ObjectNode params = data.putObject("params");
        ArrayNode files = params.putArray("files");
        if (req.getFiles() != null) {
            for (RemoteLogUploadRequest.UploadFile f : req.getFiles()) {
                ObjectNode file = files.addObject();
                file.put("module", f.getModule());
                file.put("object_key", sn + "/" + f.getModule() + "/" + f.getBootIndex() + ".log");
                ArrayNode list = file.putArray("list");
                list.addObject().put("boot_index", f.getBootIndex());
            }
        }
        message.set("data", data);
        publish(sn, message);
    }

    /**
     * 取消日志上传
     */
    @ApiLog("取消上传")
    public void cancelUpload(String sn, RemoteLogListRequest req) {
        checkAccess(sn);
        ObjectNode message = baseMessage("fileupload_update");
        ObjectNode data = objectMapper.createObjectNode();
        data.put("status", "cancel");
        ArrayNode modules = data.putArray("module_list");
        if (req.getModuleList() == null || req.getModuleList().isEmpty()) {
            modules.add("0");
            modules.add("3");
        } else {
            req.getModuleList().forEach(modules::add);
        }
        message.set("data", data);
        publish(sn, message);
    }

    /**
     * 处理 services_reply 回执：fileupload_list 回执推文件列表，其余记录结果
     */
    public void handleReply(String sn, String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析远程日志回执失败 sn={} payload={}", sn, payload, e);
            return;
        }
        String method = root.path("method").asText(null);
        JsonNode data = root.path("data");
        if ("fileupload_list".equals(method)) {
            // 文件列表 → 实时推送给前端
            messagingTemplate.convertAndSend(REMOTE_LOG_TOPIC_PREFIX + sn + "/remotelog/list", data);
        } else if("fileupload_start".equals(method)){
            int result = data.path("result").asInt(-1);
            if (result == 0) {
                log.info("设备开始上传日志文件 sn={} method={} result={}（result: 0 成功 / 1 失败 / 2 超时）", sn, method, result);
            } else{
                log.error("请求设备上传日志失败 sn={} method={} result={}（result: 0 成功 / 1 失败 / 2 超时）", sn, method, result);
            }
        } else if ("fileupload_update".equals(method)) {
            int result = data.path("result").asInt(-1);
            if (result == 0) {
                log.info("设备取消上传日志文件 sn={} method={} result={}（result: 0 成功 / 1 失败 / 2 超时）", sn, method, result);
            } else{
                log.error("请求设备取消上传日志失败 sn={} method={} result={}（result: 0 成功 / 1 失败 / 2 超时）", sn, method, result);
            }
        }
    }

    /**
     * 处理文件上传进度（fileupload_progress，events 主题）：推送给前端
     */
    public void handleProgress(String sn, String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析 fileupload_progress 失败 sn={} payload={}", sn, payload, e);
            return;
        }
        JsonNode output = root.path("data").path("output");
        if (output == null || !output.isObject()) {
            return;
        }
        log.info("远程日志上传进度 sn={}", sn);
        messagingTemplate.convertAndSend(REMOTE_LOG_TOPIC_PREFIX + sn + "/remotelog", output);
    }

    /**
     * 校验设备存在 + 机构隔离
     */
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
     * 构造 services 消息信封（公共 tid/bid/timestamp/method）：
     * tid 复用 traceId，设备回执原样带回，与本次操作全链路关联
     */
    private ObjectNode baseMessage(String method) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("tid", TraceContext.traceIdOrNew());
        message.put("bid", UUID.randomUUID().toString());
        message.put("timestamp", System.currentTimeMillis());
        message.put("method", method);
        return message;
    }

    /**
     * 发布到 services 主题（报文由 MqttPublisher 统一记录）
     */
    private void publish(String sn, ObjectNode message) {
        String topic = "thing/product/" + sn + "/services";
        String method = message.get("method").asText();
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(message), "下发远程日志指令 " + method);
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.PROPERTY_SET_FAILED);
        }
        log.info("已下发远程日志命令 sn={} method={}", sn, method);
    }
}