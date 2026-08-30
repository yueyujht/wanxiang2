package cn.wanxing.device.live.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.live.dto.LiveCameraChangeRequest;
import cn.wanxing.device.live.dto.LiveLensChangeRequest;
import cn.wanxing.device.live.dto.LiveQualityRequest;
import cn.wanxing.device.live.dto.LiveStartRequest;
import cn.wanxing.device.live.dto.LiveStopRequest;
import cn.wanxing.device.live.service.LiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 直播控制接口（执行结果经 services_reply 推送到 /topic/device/{sn}/live；仅配置 MQTT 后生效）
 */
@RestController
@RequestMapping("/device/{sn}/live")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class LiveController {

    private final LiveService liveService;

    /**
     * 开始直播
     */
    @SaCheckPermission(PermissionConst.LIVE_CONTROL)
    @PostMapping("/start")
    public Result<Boolean> start(@PathVariable String sn, @Valid @RequestBody LiveStartRequest req) {
        return Result.success(liveService.start(sn, req));
    }

    /**
     * 停止直播
     */
    @SaCheckPermission(PermissionConst.LIVE_CONTROL)
    @PostMapping("/stop")
    public Result<Boolean> stop(@PathVariable String sn, @Valid @RequestBody LiveStopRequest req) {
        return Result.success(liveService.stop(sn, req));
    }

    /**
     * 设置直播清晰度
     */
    @SaCheckPermission(PermissionConst.LIVE_CONTROL)
    @PostMapping("/quality")
    public Result<Boolean> setQuality(@PathVariable String sn, @Valid @RequestBody LiveQualityRequest req) {
        return Result.success(liveService.setQuality(sn, req));
    }

    /**
     * 设置直播镜头
     */
    @SaCheckPermission(PermissionConst.LIVE_CONTROL)
    @PostMapping("/lens")
    public Result<Boolean> changeLens(@PathVariable String sn, @Valid @RequestBody LiveLensChangeRequest req) {
        return Result.success(liveService.changeLens(sn, req));
    }

    /**
     * 直播相机切换（FPV 舱内/舱外）
     */
    @SaCheckPermission(PermissionConst.LIVE_CONTROL)
    @PostMapping("/camera")
    public Result<Boolean> changeCamera(@PathVariable String sn, @Valid @RequestBody LiveCameraChangeRequest req) {
        return Result.success(liveService.changeCamera(sn, req));
    }
}
