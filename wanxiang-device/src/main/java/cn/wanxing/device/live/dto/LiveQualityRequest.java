package cn.wanxing.device.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 设置直播清晰度请求
 */
@Getter
@Setter
public class LiveQualityRequest {

    /** 直播视频流 ID */
    @NotBlank(message = "直播视频流 ID 不能为空")
    private String videoId;

    /** 直播质量：0 自适应 / 1 流畅 / 2 标清 / 3 高清 / 4 超清 */
    @NotNull(message = "直播质量不能为空")
    private Integer videoQuality;
}
