package cn.wanxing.device.wayline.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.wayline.entity.WaylineFile;
import cn.wanxing.device.wayline.service.WaylineFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 航线文件接口：KMZ 上传/列表/删除/下载。
 * 下载接口（/files/{id}/download）供机场设备调用，已在 SaToken 放行，返回原始文件内容而非 Result 包装。
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class WaylineFileController {

    private final WaylineFileService waylineFileService;

    /**
     * 航线文件列表（机构用户可见本机构 + 全局，超管全部）
     */
    @SaCheckPermission(PermissionConst.ROUTE_READ)
    @GetMapping("/wayline/files")
    public Result<List<WaylineFile>> list() {
        return Result.success(waylineFileService.listFiles());
    }

    /**
     * 上传航线文件（KMZ，DJI WPML 标准格式）
     */
    @SaCheckPermission(PermissionConst.ROUTE_CREATE)
    @PostMapping("/wayline/files")
    public Result<WaylineFile> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "wayline.kmz" : file.getOriginalFilename();
        return Result.success(waylineFileService.upload(name, file.getBytes()));
    }

    /**
     * 删除航线文件（全局文件仅超管可删；存在进行中任务引用时拒绝）
     */
    @SaCheckPermission(PermissionConst.ROUTE_DELETE)
    @DeleteMapping("/wayline/files/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(waylineFileService.deleteFile(id));
    }

    /**
     * 下载航线文件（机场设备经 flighttask_resource_get 获取本地址后调用，返回原始 KMZ 内容）
     */
    @GetMapping("/wayline/files/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        WaylineFile file = waylineFileService.getFileForDownload(id);
        if (file == null) {
            // 设备侧消费，用标准 404 而非 Result 包装，便于设备端判别
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.getContent());
    }
}
