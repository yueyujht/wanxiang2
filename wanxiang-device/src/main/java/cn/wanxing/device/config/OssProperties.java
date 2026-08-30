package cn.wanxing.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储配置：设备直传的目标存储（阿里云 OSS / AWS S3 / MinIO）。
 *
 * <p>对应 application.yml 中的 {@code wanxiang.oss.*}，用于远程日志 fileupload_start
 * 与媒体上传 storage_config_get 下发临时凭证。
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

    /** 媒体文件的存储 Key 前缀（storage_config_get 下发，缺省按网关 SN 分桶内目录） */
    private String objectKeyPrefix;
}
