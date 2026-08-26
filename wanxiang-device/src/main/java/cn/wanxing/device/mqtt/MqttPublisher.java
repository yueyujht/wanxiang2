package cn.wanxing.device.mqtt;

import cn.wanxing.device.config.MqttConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * MQTT 消息发布器：把消息发到指定主题（如回复设备的 requests_reply）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class MqttPublisher {

    private final MessageChannel outboundChannel;

    public MqttPublisher(@Qualifier(MqttConfig.OUTBOUND_CHANNEL) MessageChannel outboundChannel) {
        this.outboundChannel = outboundChannel;
    }

    /**
     * 发布一条文本消息到指定主题
     */
    public void publish(String topic, String payload) {
        // 把目标主题放进消息头 MqttHeaders.TOPIC，出站处理器据此发布到对应主题
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();
        outboundChannel.send(message);
        log.debug("发布 MQTT 消息 topic={} payload={}", topic, payload);
    }
}
