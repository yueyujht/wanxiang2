package cn.wanxing.device.remotelog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.remotelog.dto.RemoteLogListRequest;
import cn.wanxing.device.remotelog.dto.RemoteLogUploadRequest;
import cn.wanxing.device.remotelog.service.RemoteLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 远程日志接口
 */
@RestController
@RequestMapping("/device/{sn}/log")
@RequiredArgsConstructor
public class RemoteLogController {

    private final RemoteLogService remoteLogService;

    /**
     * 查询设备可上传日志文件列表
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PostMapping("/list")
    public Result<Boolean> list(@PathVariable String sn, @RequestBody(required = false) RemoteLogListRequest req) {
        remoteLogService.listLogs(sn, req);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 发起日志文件上传
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PostMapping("/upload")
    public Result<Boolean> upload(@PathVariable String sn, @Valid @RequestBody RemoteLogUploadRequest req) {
        remoteLogService.startUpload(sn, req);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 取消日志上传
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PostMapping("/cancel")
    public Result<Boolean> cancel(@PathVariable String sn, @RequestBody(required = false) RemoteLogListRequest req) {
        remoteLogService.cancelUpload(sn, req == null ? new RemoteLogListRequest() : req);
        return Result.success(Boolean.TRUE);
    }
}