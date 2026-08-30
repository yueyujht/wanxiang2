package cn.wanxing.device.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义飞行区配置，对应 application.yml 中的 {@code wanxiang.flight-area.*}。
 *
 * <p>设备经 requests: flight_areas_get 拉取飞行区文件列表，文件下载走平台 HTTP 接口
 * （文件存库，不经对象存储），该地址必须为机场可达的平台地址。
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "wanxiang.flight-area")
public class FlightAreaProperties {

    /** 平台对外基地址（如 http://192.168.1.10:8080），设备经此下载飞行区文件 */
    private String baseUrl;
}
