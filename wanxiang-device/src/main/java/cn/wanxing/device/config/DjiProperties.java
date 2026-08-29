package cn.wanxing.device.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DJI 上云 API 应用配置，对应 application.yml 中的 {@code wanxiang.dji.*}。
 *
 * <p>这些值来自 DJI 开发者平台创建「上云 API」应用后拿到的 AppId / AppKey / AppLicense。
 * 官方协议要求 config 回执中 app_id/app_key/app_license 必填——未配置时设备上云必然失败，
 * 因此启动时自检告警，配置后需重启生效。
 */
@Slf4j
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

    /** NTP 服务端口号（默认 123） */
    private Integer ntpServerPort = 123;

    @PostConstruct
    public void checkRequired() {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(appKey) || StringUtils.isBlank(appLicense)) {
            log.warn("上云 API 凭据未配置（wanxiang.dji.app-id/app-key/app-license），"
                    + "设备 config 请求将拿到空凭据，Pilot 2 上云流程会失败");
        }
    }
}
