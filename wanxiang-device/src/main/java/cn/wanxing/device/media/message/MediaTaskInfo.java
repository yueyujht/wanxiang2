package cn.wanxing.device.media.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 飞行任务媒体上传进度
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaTaskInfo {

    /** 该飞行架次当前已上传媒体数量 */
    private Integer uploadedFileCount;

    /** 该飞行架次拍摄媒体总数量 */
    private Integer expectedFileCount;

    /** 飞行类型：0 航线任务 / 1 一键起飞任务 */
    private Integer flightType;
}
