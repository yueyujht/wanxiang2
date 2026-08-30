package cn.wanxing.device.live.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 设置直播镜头请求（不影响直播进程）
 */
@Getter
@Setter
public class LiveLensChangeRequest {

    /** 直播视频流镜头类型：normal 默认 / wide 广角 / zoom 变焦 / ir 红外 */
    @NotBlank(message = "镜头类型不能为空")
    private String videoType;
}
