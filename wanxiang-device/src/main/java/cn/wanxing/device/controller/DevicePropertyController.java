package cn.wanxing.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.dto.request.DevicePropertySetRequest;
import cn.wanxing.device.service.DevicePropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备属性设置接口（仅配置 MQTT 后生效）
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DevicePropertyController {

    private final DevicePropertyService devicePropertyService;

    /**
     * 设置设备属性（下发 property/set）
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PutMapping("/{sn}/property")
    public Result<Boolean> setProperty(@PathVariable String sn, @Valid @RequestBody DevicePropertySetRequest req) {
        devicePropertyService.setProperty(sn, req);
        return Result.success(Boolean.TRUE);
    }
}
