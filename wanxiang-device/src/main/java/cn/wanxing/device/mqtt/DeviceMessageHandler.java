package cn.wanxing.device.mqtt;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 设备消息统一入口：从入站通道接收原始消息，按主题类型派发到对应处理逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DeviceMessageHandler {

    private final TopologyService topologyService;

    private final BindingService bindingService;

    private final DevicePropertyService propertyService;

    private final DeviceStateService stateService;

    private final DeviceOsdService osdService;

    private final AlarmService alarmService;

    private final FirmwareService firmwareService;

    private final RemoteLogService remoteLogService;

    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = MqttConfig.INBOUND_CHANNEL)
    public void onMessage(Message<?> message) throws JsonProcessingException {
        // 1.获取 topic 与消息体
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        byte[] payload = (byte[]) message.getPayload();
        String body = payload == null ? "" : new String(payload, StandardCharsets.UTF_8);

        // 2.识别消息类型并分发
        DeviceTopicType type = DeviceTopicType.fromTopic(topic);
        switch (type) {
            case ONLINE_OFFLINE -> topologyService.handleStatus(extractSn(topic), body);
            case REQUESTS -> bindingService.handleRequest(topic, body);
            case PROPERTY_SET_REPLY -> propertyService.handleReply(extractSn(topic), body);
            case STATE -> stateService.handleState(extractSn(topic), body);
            case OSD -> osdService.handleOsd(extractSn(topic), body);
            case EVENTS -> routeEvents(extractSn(topic), body);
            case SERVICES_REPLY -> routeServicesReply(extractSn(topic), body);
            default -> log.debug("忽略未知消息 topic={}", topic);
        }
    }

    /**
     * events 主题按 method 分发：hms 告警，ota_progress 固件升级进度，fileupload_progress 远程日志进度
     */
    private void routeEvents(String sn, String body) {
        String method = extractMethod(body);
        if ("hms".equals(method)) {
            alarmService.handleEvents(sn, body);
        } else if ("ota_progress".equals(method)) {
            firmwareService.handleProgress(sn, body);
        } else if ("fileupload_progress".equals(method)) {
            remoteLogService.handleProgress(sn, body);
        } else {
            log.debug("忽略未知 events method={} sn={}", method, sn);
        }
    }

    /**
     * services_reply 主题按 method 分发：ota_create 固件，fileupload_* 远程日志
     */
    private void routeServicesReply(String sn, String body) {
        String method = extractMethod(body);
        if ("ota_create".equals(method)) {
            firmwareService.handleReply(sn, body);
        } else if (method != null && method.startsWith("fileupload_")) {
            remoteLogService.handleReply(sn, body);
        } else {
            log.debug("忽略未知 services_reply method={} sn={}", method, sn);
        }
    }

    /**
     * 从消息体提取 method 字段
     */
    private String extractMethod(String body) {
        try {
            return objectMapper.readTree(body).path("method").asText(null);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从主题中解析设备序列号：{prefix}product/{sn}/...（sys 与 thing 前缀通用）
     */
    private String extractSn(String topic) {
        String product = DeviceTopicConst.PRODUCT;
        int start = topic.indexOf(product) + product.length();
        int end = topic.indexOf("/", start);
        return topic.substring(start, end);
    }
}