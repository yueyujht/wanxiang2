package cn.wanxing.device.airsense.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.airsense.dto.AirsenseQueryRequest;
import cn.wanxing.device.airsense.entity.AirsenseWarning;
import cn.wanxing.device.airsense.service.AirsenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AirSense 空域告警接口
 */
@RestController
@RequestMapping("/device/airsense")
@RequiredArgsConstructor
public class AirsenseController {

    private final AirsenseService airsenseService;

    /**
     * AirSense 告警列表（分页 + 按设备/等级筛选）
     */
    @SaCheckPermission(PermissionConst.ALARM_READ)
    @GetMapping("/list")
    public MultiResult<AirsenseWarning> list(AirsenseQueryRequest req) {
        return airsenseService.listWarnings(req);
    }
}
