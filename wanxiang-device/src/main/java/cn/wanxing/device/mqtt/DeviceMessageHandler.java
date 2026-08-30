package cn.wanxing.device.mqtt;

import cn.wanxing.common.log.TraceContext;
import cn.wanxing.device.airsense.service.AirsenseService;
import cn.wanxing.device.alarm.service.AlarmService;
import cn.wanxing.device.bind.BindingService;
import cn.wanxing.device.config.MqttConfig;
import cn.wanxing.device.firmware.service.FirmwareService;
import cn.wanxing.device.status.service.DevicePropertyService;
import cn.wanxing.device.remotelog.service.RemoteLogService;
import cn.wanxing.device.status.service.DeviceOsdService;
import cn.wanxing.device.status.service.DeviceStateService;
import cn.wanxing.device.status.service.TopologyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 设备消息统一入口：从入站通道接收原始消息，按主题类型派发到对应处理逻辑。
 *
 * <p>收发报文日志统一在这里记录（每条消息一条，含场景），各业务 handler 不再自行记录报文。
 * traceId 用设备报文自带的 tid（设备回执原样带回，可与设备侧日志互查），处理完在 finally 清理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DeviceMessageHandler {

    /** 日志单字段最大长度，超出截断，避免大消息刷屏 */
    private static final int MAX_LOG_LENGTH = 2000;

    private final TopologyService topologyService;

    private final BindingService bindingService;

    private final DevicePropertyService propertyService;

    private final DeviceStateService stateService;

    private final DeviceOsdService osdService;

    private final AlarmService alarmService;

    private final AirsenseService airsenseService;

    private final FirmwareService firmwareService;

    private final RemoteLogService remoteLogService;

    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = MqttConfig.INBOUND_CHANNEL)
    public void onMessage(Message<?> message) {
        // 1.获取 topic 与消息体
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        byte[] payload = (byte[]) message.getPayload();
        String body = payload == null ? "" : new String(payload, StandardCharsets.UTF_8);

        // 2.提取信封字段：tid 用作全链路 traceId，method 用于场景识别
        //   消息不是合法 JSON 时告警（设备端异常信号），继续分发由各 handler 的解析兜底处理
        String tid = null;
        String method = null;
        try {
            JsonNode root = objectMapper.readTree(body);
            tid = root.path("tid").asText(null);
            method = root.path("method").asText(null);
        } catch (JsonProcessingException e) {
            log.warn("[MQTT] 消息不是合法 JSON topic={} payload={}", topic, truncate(body));
        }
        // 消息类型
        DeviceTopicType type = DeviceTopicType.fromTopic(topic);
        // 消息业务描述
        MqttScenario scenario = MqttScenario.of(type, method);
        // 从topic提取sn
        String sn = extractSn(topic);

        // 3.设备 tid 写入 MDC，本条消息处理期间所有日志自动携带，处理完清理
        TraceContext.setTraceId(tid != null && !tid.isBlank() ? tid : UUID.randomUUID().toString());

        log.info("[MQTT] 收到 topic={} sn={} 场景={} method={} payload={}",
                topic, sn, scenario.label(), method, truncate(body));
        long start = System.currentTimeMillis();
        try {
            // 4.识别消息类型并分发
            dispatch(type, topic, sn, method, body);
            log.debug("[MQTT] 处理完成 场景={} sn={} cost={}ms", scenario.label(), sn, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            // 吞掉异常但留下完整现场：单条坏消息不能中断 MQTT 监听线程（QoS1 下重抛也不会重投递）
            log.error("[MQTT] 处理异常 场景={} sn={} cost={}ms", scenario.label(), sn, System.currentTimeMillis() - start, t);
        } finally {
            TraceContext.clear();
        }
    }

    /**
     * 按主题类型派发：events / services_reply 再按 method 二次路由
     */
    private void dispatch(DeviceTopicType type, String topic, String sn, String method, String body) {
        switch (type) {
            case ONLINE_OFFLINE -> topologyService.handleStatus(sn, body);
            case REQUESTS -> bindingService.handleRequest(topic, body);
            case PROPERTY_SET_REPLY -> propertyService.handleReply(sn, body);
            case STATE -> stateService.handleState(sn, body);
            case OSD -> osdService.handleOsd(sn, body);
            case EVENTS -> routeEvents(sn, method, body);
            case SERVICES_REPLY -> routeServicesReply(sn, method, body);
            default -> log.warn("[MQTT] 未知的消息主题，已忽略 topic={}", topic);
        }
    }

    /**
     * events 主题按 method 分发：hms 告警，ota_progress 固件升级进度，fileupload_progress 远程日志进度，
     * airsense_warning 空域告警（ADS-B 周边航班）
     */
    private void routeEvents(String sn, String method, String body) {
        if ("hms".equals(method)) {
            alarmService.handleEvents(sn, body);
        } else if ("ota_progress".equals(method)) {
            firmwareService.handleProgress(sn, body);
        } else if ("fileupload_progress".equals(method)) {
            remoteLogService.handleProgress(sn, body);
        } else if ("airsense_warning".equals(method)) {
            airsenseService.handleWarning(sn, body);
        } else {
            log.warn("[MQTT] 忽略未知 events method={} sn={}", method, sn);
        }
    }

    /**
     * services_reply 主题按 method 分发：ota_create 固件，fileupload_* 远程日志
     */
    private void routeServicesReply(String sn, String method, String body) {
        if ("ota_create".equals(method)) {
            firmwareService.handleReply(sn, body);
        } else if (method != null && method.startsWith("fileupload_")) {
            remoteLogService.handleReply(sn, body);
        } else {
            log.warn("[MQTT] 忽略未知 services_reply method={} sn={}", method, sn);
        }
    }

    /**
     * 从主题中解析设备序列号：{prefix}product/{sn}/...（sys 与 thing 前缀通用）
     */
    private String extractSn(String topic) {
        if (topic == null) {
            return null;
        }
        int start = topic.indexOf(DeviceTopicConst.PRODUCT);
        if (start < 0) {
            return null;
        }
        start += DeviceTopicConst.PRODUCT.length();
        int end = topic.indexOf("/", start);
        return end < 0 ? topic.substring(start) : topic.substring(start, end);
    }

    /**
     * 报文超长时截断，避免大消息刷屏
     */
    private String truncate(String body) {
        if (body == null || body.length() <= MAX_LOG_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_LOG_LENGTH) + "...(截断)";
    }
}
