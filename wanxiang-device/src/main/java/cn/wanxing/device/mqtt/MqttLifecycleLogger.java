package cn.wanxing.device.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MQTT 连接生命周期守护：记录与 broker 的连接建立/断开，并在连接丢失后自动恢复。
 *
 * <p>连接断开期间所有设备都会「集体离线」，这类问题从设备日志看不出来，必须在平台侧留证据，
 * 因此断开记 ERROR 并随主日志落 ERROR 文件。
 *
 * <p>自动恢复：Paho 的自动重连只在「曾连接成功后断开」的场景生效，
 * <b>应用启动时 broker 不在线（首连失败）不会触发重连</b>，适配器会停在断开状态不再尝试。
 * 这里用事件跟踪健康状态（订阅成功=健康 / 连接失败=不健康），守护任务发现不健康时
 * 重启入站适配器重新发起连接，broker 恢复后最多一个周期内自动接上。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class MqttLifecycleLogger {

    /** 恢复检查周期：与断开发现延迟叠加，broker 恢复后最多约 1 个周期内重新接上 */
    private static final long CHECK_INTERVAL_MS = 30_000;

    private final MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter;

    /** 连接健康标记：订阅成功为健康，连接失败（含首连失败）为不健康 */
    private volatile boolean healthy = true;

    /**
     * 连接成功并完成订阅（每个订阅主题一条事件，message 里含主题）
     */
    @EventListener
    public void onSubscribed(MqttSubscribedEvent event) {
        if (!healthy) {
            log.info("[MQTT] broker 重新连接成功");
        }
        healthy = true;
        log.info("[MQTT] 已订阅主题 detail={}", event.getMessage());
    }

    /**
     * 连接失败（broker 宕机/网络抖动/首连时 broker 不在线），标记不健康等待守护任务恢复
     */
    @EventListener
    public void onConnectionFailed(MqttConnectionFailedEvent event) {
        healthy = false;
        Throwable cause = event.getCause();
        log.error("[MQTT] broker 连接失败，将由守护任务自动恢复", cause != null ? cause : event);
    }

    /**
     * 连接恢复守护：不健康时重启入站适配器重新连接（stop/start 均吞异常，失败等下轮重试）
     */
    @Scheduled(fixedDelay = CHECK_INTERVAL_MS, initialDelay = CHECK_INTERVAL_MS)
    public void recoverIfUnhealthy() {
        if (healthy) {
            return;
        }
        log.warn("[MQTT] 检测到连接未建立，尝试重启 MQTT 连接...");
        try {
            mqttInboundAdapter.stop();
        } catch (Exception e) {
            log.debug("[MQTT] 适配器 stop 异常（未连接状态下可忽略）", e);
        }
        try {
            mqttInboundAdapter.start();
        } catch (Exception e) {
            log.warn("[MQTT] 重启连接仍失败，等待下轮重试 error={}", e.getMessage());
        }
    }
}
