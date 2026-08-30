package cn.wanxing.device.media.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 媒体上传结果消息的 data 部分：文件信息 + 任务上传进度
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaUploadData {

    /** 文件信息 */
    private MediaFileInfo file;

    /** 飞行任务信息（手动上传时为空） */
    private MediaTaskInfo flightTask;
}
