package cn.wanxing.device.live.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 直播相机切换请求（Dock 3，FPV 相机舱内/舱外切换）
 */
@Getter
@Setter
public class LiveCameraChangeRequest {

    /** 直播视频流 ID */
    private String videoId;

    /** FPV 位置：0 舱内 / 1 舱外 */
    @NotNull(message = "FPV 位置不能为空")
    private Integer cameraPosition;
}
