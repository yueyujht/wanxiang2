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
 *
 * <p>出站报文统一在这里记录（含场景），发送失败记 ERROR——掉线/ broker 异常时下发是否成功
 * 是排查设备无响应的关键证据。日志经 logger {@code cn.wanxing.device.mqtt} 落入独立 MQTT 文件。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class MqttPublisher {

    /** 日志单字段最大长度，超出截断，避免大消息刷屏 */
    private static final int MAX_LOG_LENGTH = 2000;

    private final MessageChannel outboundChannel;

    public MqttPublisher(@Qualifier(MqttConfig.OUTBOUND_CHANNEL) MessageChannel outboundChannel) {
        this.outboundChannel = outboundChannel;
    }

    /**
     * 发布一条文本消息到指定主题（无场景标注）
     */
    public void publish(String topic, String payload) {
        publish(topic, payload, null);
    }

    /**
     * 发布一条文本消息到指定主题，并记录带场景的发送日志
     *
     * @param scenario 业务场景（如「下发固件升级」），null 时不标注
     */
    public void publish(String topic, String payload, String scenario) {
        // 把目标主题放进消息头 MqttHeaders.TOPIC，出站处理器据此发布到对应主题
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();
        long start = System.currentTimeMillis();
        try {
            outboundChannel.send(message);
            log.info("[MQTT] 发送 topic={} 场景={} cost={}ms payload={}",
                    topic, scenario, System.currentTimeMillis() - start, truncate(payload));
        } catch (RuntimeException e) {
            log.error("[MQTT] 发送失败 topic={} 场景={} error={}", topic, scenario, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 报文超长时截断，避免大消息刷屏
     */
    private String truncate(String payload) {
        if (payload == null || payload.length() <= MAX_LOG_LENGTH) {
            return payload;
        }
        return payload.substring(0, MAX_LOG_LENGTH) + "...(截断)";
    }
}
