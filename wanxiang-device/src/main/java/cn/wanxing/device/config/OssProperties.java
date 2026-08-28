package cn.wanxing.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储配置：设备上传日志的目标存储（阿里云 OSS / AWS S3 / MinIO）。
 *
 * <p>对应 application.yml 中的 {@code wanxiang.oss.*}，用于 fileupload_start 下发凭证。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wanxiang.oss")
public class OssProperties {

    /** 云厂商：ali / aws / minio */
    private String provider = "ali";

    /** 对象存储桶名称 */
    private String bucket;

    /** 数据中心地域 */
    private String region;

    /** 对外访问域名 */
    private String endpoint;

    /** 访问密钥 ID */
    private String accessKeyId;

    /** 秘密访问密钥 */
    private String accessKeySecret;

    /** 会话凭证（STS） */
    private String securityToken;
}
