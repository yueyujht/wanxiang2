package cn.wanxing.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.service.DeviceOsdService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备 OSD 遥测查询接口（仅配置 MQTT 后生效）
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DeviceOsdController {

    private final DeviceOsdService deviceOsdService;

    /**
     * 查询设备最新 OSD 遥测（实时监控用，无数据返回 null）
     */
    @SaCheckPermission(PermissionConst.DEVICE_READ)
    @GetMapping("/{sn}/osd")
    public Result<JsonNode> latestOsd(@PathVariable String sn) {
        return Result.success(deviceOsdService.getLatestOsd(sn));
    }
}