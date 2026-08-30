package cn.wanxing.device.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 开始直播请求
 */
@Getter
@Setter
public class LiveStartRequest {

    /** 直播视频流 ID，格式 {sn}/{camera_index}/{video_index}（如 1ZNDH1D0010098/39-0-7/normal-0），取自设备 state 上报的 live_capacity */
    @NotBlank(message = "直播视频流 ID 不能为空")
    private String videoId;

    /** 直播协议类型：1 RTMP / 3 GB28181 / 4 WebRTC（WHIP） */
    @NotNull(message = "直播协议类型不能为空")
    private Integer urlType;

    /** 直播质量：0 自适应 / 1 流畅 / 2 标清 / 3 高清 / 4 超清 */
    @NotNull(message = "直播质量不能为空")
    private Integer videoQuality;

    /** 直播参数；不传则按协议类型从 wanxiang.live 配置生成 */
    private String url;
}
