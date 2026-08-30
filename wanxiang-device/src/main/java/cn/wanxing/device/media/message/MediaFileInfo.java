package cn.wanxing.device.media.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 上报的单个媒体文件信息
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaFileInfo {

    /** 文件在对象存储桶的 Key */
    private String objectKey;

    /** 文件的业务路径 */
    private String path;

    /** 文件名称 */
    private String name;

    /** 云云对接存储桶 ID（未走云云对接时为 DEFAULT） */
    private String cloudToCloudId;

    /** 业务扩展信息 */
    private Ext ext;

    /** 媒体元数据 */
    private Metadata metadata;

    /** 业务扩展信息 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ext {

        /** 任务 ID */
        private String flightId;

        /** 飞行器型号枚举（domain-type-subtype） */
        private String droneModelKey;

        /** 负载型号枚举（domain-type-subtype） */
        private String payloadModelKey;

        /** 是否原图 */
        private Boolean isOriginal;
    }

    /** 媒体元数据 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {

        /** 云台偏航角（度） */
        private Double gimbalYawDegree;

        /** 拍摄绝对高度（米） */
        private Double absoluteAltitude;

        /** 拍摄相对高度（米） */
        private Double relativeAltitude;

        /** 媒体拍摄时间（示例 2021-05-10 16:04:20，个别固件为 ISO8601） */
        private String createTime;

        /** 拍摄位置 */
        private ShootPosition shootPosition;
    }

    /** 拍摄位置 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShootPosition {

        /** 纬度 */
        private Double lat;

        /** 经度 */
        private Double lng;
    }
}
