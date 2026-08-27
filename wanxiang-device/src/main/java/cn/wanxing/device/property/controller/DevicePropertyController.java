package cn.wanxing.device.property.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.property.dto.DevicePropertySetRequest;
import cn.wanxing.device.property.dto.DevicePropertySchemaVO;
import cn.wanxing.device.property.service.DevicePropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * 查询可设置属性字典（供前端渲染设置界面）
     */
    @SaCheckPermission(PermissionConst.DEVICE_READ)
    @GetMapping("/property/schema")
    public Result<List<DevicePropertySchemaVO>> listSchema() {
        return Result.success(devicePropertyService.listSchema());
    }

    /**
     * 设置设备属性（下发 property/set）
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PutMapping("/{sn}/property")
    public Result<Boolean> setProperty(@PathVariable String sn, @Valid @RequestBody DevicePropertySetRequest req) {
        Boolean response = devicePropertyService.setProperty(sn, req);
        return Result.success(response);
    }
}