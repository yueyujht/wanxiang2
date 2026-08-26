package cn.wanxing.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DJI 上云 API 应用配置，对应 application.yml 中的 {@code wanxiang.dji.*}。
 *
 * <p>这些值来自 DJI 开发者平台创建「上云 API」应用后拿到的 AppId / AppKey / AppLicense。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wanxiang.dji")
public class DjiProperties {

    /** App ID */
    private String appId;

    /** App Key */
    private String appKey;

    /** App License */
    private String appLicense;

    /** NTP 时间服务器地址 */
    private String ntpServerHost;
}
