package cn.wanxing.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQTT 连接配置，对应 application.yml 中的 {@code wanxiang.mqtt.*}。
 *
 * <p>这些值来自 DJI 开发者平台：创建应用后，在「上云」配置里能看到网关地址与账号密码。
 */
@Data
@ConfigurationProperties(prefix = "wanxiang.mqtt")
public class MqttProperties {

    /** MQTT 网关地址，如 tcp://xxx.xxx.com:8883 */
    private String brokerUrl;

    /** 账号 */
    private String username;

    /** 密码 */
    private String password;

    /** 订阅主题（逗号分隔），设备消息从这些主题进来 */
    private String inboundTopic = "sys/product/+/status,thing/product/+/osd,thing/product/+/state,thing/product/+/events,thing/product/+/requests,thing/product/+/property/set_reply";

    /** 客户端 ID 前缀（后接随机数保证唯一） */
    private String clientId = "wanxiang";
}