package cn.wanxing.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.dto.vo.DeviceVO;
import cn.wanxing.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备管理接口
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 设备列表（机构管理员只看本机构）
     */
    @SaCheckPermission(PermissionConst.DEVICE_READ)
    @GetMapping("/list")
    public Result<List<DeviceVO>> list() {
        return Result.success(deviceService.list());
    }
}
