package cn.wanxing.device.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 直播推流配置，对应 application.yml 中的 {@code wanxiang.live.*}。
 *
 * <p>开始直播请求未显式携带 url 时，按协议类型从这里兜底生成推流参数；
 * RTMP/WHIP 的流名自动取 video_id（/ 替换为 _），GB28181 为服务器端固定参数串。
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "wanxiang.live")
public class LiveProperties {

    /** RTMP 推流基址（如 rtmp://ip:1935/live），实际推流地址 = 基址/流名 */
    private String rtmpUrl;

    /** WebRTC WHIP 端点前缀（如 http://ip:1985/rtc/v1/whip），实际地址 = 前缀?app=live&stream=流名 */
    private String whipUrl;

    /** GB28181 完整参数串（serverIP=..&serverPort=..&serverID=..&agentID=..&agentPassword=..&localPort=..&channel=..） */
    private String gb28181Url;
}
