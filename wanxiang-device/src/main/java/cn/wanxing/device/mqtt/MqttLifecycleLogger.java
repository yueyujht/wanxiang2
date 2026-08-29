package cn.wanxing.device.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Component;

/**
 * MQTT 连接生命周期日志：记录与 broker 的连接建立/断开。
 *
 * <p>连接断开期间所有设备都会「集体离线」，这类问题从设备日志看不出来，必须在平台侧留证据，
 * 因此断开记 ERROR 并随主日志落 ERROR 文件。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class MqttLifecycleLogger {

    /**
     * 连接成功并完成订阅（每个订阅主题一条事件，message 里含主题）
     */
    @EventListener
    public void onSubscribed(MqttSubscribedEvent event) {
        log.info("[MQTT] broker 连接成功，已订阅主题 detail={}", event.getMessage());
    }

    /**
     * 连接断开（broker 宕机/网络抖动等），适配器会自动重连
     */
    @EventListener
    public void onConnectionFailed(MqttConnectionFailedEvent event) {
        Throwable cause = event.getCause();
        log.error("[MQTT] broker 连接断开，将自动重连", cause != null ? cause : event);
    }
}
