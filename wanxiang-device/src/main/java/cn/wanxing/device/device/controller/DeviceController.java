package cn.wanxing.device.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.device.dto.DeviceQueryRequest;
import cn.wanxing.device.device.dto.DeviceRenameRequest;
import cn.wanxing.device.device.service.DeviceService;
import cn.wanxing.device.device.dto.DeviceVO;
import cn.wanxing.device.state.entity.DeviceState;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备管理接口
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 设备列表（分页 + 筛选；机构管理员只看本机构）
     */
    @SaCheckPermission(PermissionConst.DEVICE_READ)
    @GetMapping("/list")
    public MultiResult<DeviceVO> list(DeviceQueryRequest req) {
        return deviceService.list(req);
    }

    /**
     * 设备详情（单台）
     */
    @SaCheckPermission(PermissionConst.DEVICE_READ)
    @GetMapping("/{sn}")
    public Result<DeviceVO> detail(@PathVariable String sn) {
        return Result.success(deviceService.detail(sn));
    }

    /**
     * 设备最新状态（sys_device_state）
     */
    @SaCheckPermission(PermissionConst.DEVICE_READ)
    @GetMapping("/{sn}/state")
    public Result<DeviceState> state(@PathVariable String sn) {
        return Result.success(deviceService.getState(sn));
    }

    /**
     * 解绑设备（从机构移除）
     */
    @SaCheckPermission(PermissionConst.DEVICE_BIND)
    @DeleteMapping("/{sn}/bind")
    public Result<Boolean> unbind(@PathVariable String sn) {
        deviceService.unbind(sn);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 重命名设备
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PutMapping("/{sn}")
    public Result<Boolean> rename(@PathVariable String sn, @Valid @RequestBody DeviceRenameRequest req) {
        deviceService.rename(sn, req.getName());
        return Result.success(Boolean.TRUE);
    }
}