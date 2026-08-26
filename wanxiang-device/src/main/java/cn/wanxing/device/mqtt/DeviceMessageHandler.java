package cn.wanxing.device.mqtt;

import cn.wanxing.device.config.MqttConfig;
import cn.wanxing.device.constant.DeviceTopicConst;
import cn.wanxing.device.constant.DeviceTopicType;
import cn.wanxing.device.service.DevicePropertyService;
import cn.wanxing.device.service.DeviceService;
import cn.wanxing.device.service.DeviceStateService;
import cn.wanxing.device.service.DockRequestService;
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

    private final DeviceService deviceService;

    private final DockRequestService dockRequestService;

    private final DevicePropertyService devicePropertyService;

    private final DeviceStateService deviceStateService;

    @ServiceActivator(inputChannel = MqttConfig.INBOUND_CHANNEL)
    public void onMessage(Message<?> message) {
        // 1.获取 topic 与消息体
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        byte[] payload = (byte[]) message.getPayload();
        String body = payload == null ? "" : new String(payload, StandardCharsets.UTF_8);

        // 2.识别消息类型并分发
        DeviceTopicType type = DeviceTopicType.fromTopic(topic);
        switch (type) {
            case ONLINE_OFFLINE -> deviceService.handleStatus(extractSn(topic), body);
            case REQUESTS -> dockRequestService.handleRequest(topic, body);
            case PROPERTY_SET_REPLY -> devicePropertyService.handleReply(extractSn(topic), body);
            case STATE -> deviceStateService.handleState(extractSn(topic), body);
            case OSD, EVENTS, SERVICES_REPLY ->
                    log.info("暂未处理的消息 topic={} type={}", topic, type);
            default -> log.debug("忽略未知消息 topic={}", topic);
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
