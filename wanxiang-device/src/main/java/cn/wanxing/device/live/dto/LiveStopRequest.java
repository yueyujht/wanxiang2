package cn.wanxing.device.live.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 停止直播请求
 */
@Getter
@Setter
public class LiveStopRequest {

    /** 直播视频流 ID（开始直播时指定的同一路流） */
    @NotBlank(message = "直播视频流 ID 不能为空")
    private String videoId;
}
