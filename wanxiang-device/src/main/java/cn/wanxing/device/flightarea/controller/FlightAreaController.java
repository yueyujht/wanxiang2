package cn.wanxing.device.flightarea.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.flightarea.dto.FlightAreaFileRequest;
import cn.wanxing.device.flightarea.entity.FlightAreaFile;
import cn.wanxing.device.flightarea.service.FlightAreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 自定义飞行区接口：文件管理 + 同步指令。
 * 文件下载接口（/files/{id}/download）供机场设备调用，已在 SaToken 放行，返回原始文件内容而非 Result 包装。
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class FlightAreaController {

    private final FlightAreaService flightAreaService;

    /**
     * 飞行区文件列表（机构用户可见本机构 + 全局，超管全部）
     */
    @SaCheckPermission(PermissionConst.DEVICE_READ)
    @GetMapping("/flight-area/files")
    public Result<List<FlightAreaFile>> list() {
        return Result.success(flightAreaService.listFiles());
    }

    /**
     * 创建飞行区文件（官方 JSON 格式）
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PostMapping("/flight-area/files")
    public Result<FlightAreaFile> create(@Valid @RequestBody FlightAreaFileRequest req) {
        return Result.success(flightAreaService.createFile(req));
    }

    /**
     * 删除飞行区文件（全局文件仅超管可删）
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @DeleteMapping("/flight-area/files/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(flightAreaService.deleteFile(id));
    }

    /**
     * 下载飞行区文件（机场设备经 flight_areas_get 获取本地址后调用，返回原始文件内容）
     */
    @GetMapping("/flight-area/files/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        FlightAreaFile file = flightAreaService.getFileForDownload(id);
        if (file == null) {
            // 设备侧消费，用标准 404 而非 Result 包装，便于设备端判别
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(file.getContent().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 下发飞行区同步指令（通知设备拉取最新飞行区文件）
     */
    @SaCheckPermission(PermissionConst.DEVICE_CONFIG)
    @PostMapping("/{sn}/flight-area/sync")
    public Result<Boolean> sync(@PathVariable String sn) {
        return Result.success(flightAreaService.sync(sn));
    }
}
