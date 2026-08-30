package cn.wanxing.device.media.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 媒体文件上传结果消息（thing/product/{sn}/events，method=file_upload_callback）：
 * 机场把媒体文件直传对象存储后，回报文件元数据与任务上传进度。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaUploadMessage {

    private String tid;

    private String bid;

    private Long timestamp;

    private String method;

    /** 是否需要云端回执：1 需要（回 events_reply），缺省 0 */
    private Integer needReply;

    /** 文件信息与任务上传进度 */
    private MediaUploadData data;
}
