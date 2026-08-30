package cn.wanxing.device.firmware.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.firmware.dto.FirmwareUpgradeRequest;
import cn.wanxing.device.firmware.entity.FirmwareTask;
import cn.wanxing.device.firmware.service.FirmwareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 固件升级接口（仅配置 MQTT 后生效）
 */
@RestController
@RequestMapping("/device/{sn}/firmware")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class FirmwareController {

    private final FirmwareService firmwareService;

    /**
     * 下发固件升级
     */
    @SaCheckPermission(PermissionConst.FIRMWARE_UPGRADE)
    @PostMapping("/upgrade")
    public Result<Boolean> upgrade(@PathVariable String sn, @Valid @RequestBody FirmwareUpgradeRequest req) {
        Boolean response = firmwareService.upgrade(sn, req);
        return Result.success(response);
    }

    /**
     * 查询设备最近升级任务
     */
    @SaCheckPermission(PermissionConst.FIRMWARE_READ)
    @GetMapping("/tasks")
    public Result<List<FirmwareTask>> tasks(@PathVariable String sn) {
        return Result.success(firmwareService.listTasks(sn));
    }
}