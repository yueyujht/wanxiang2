package cn.wanxing.device.media.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.media.dto.MediaQueryRequest;
import cn.wanxing.device.media.entity.MediaFile;
import cn.wanxing.device.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 媒体管理接口（文件本体由设备直传对象存储，平台提供元数据索引的查询与删除；仅配置 MQTT 后生效）
 */
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class MediaController {

    private final MediaService mediaService;

    /**
     * 媒体列表（分页 + 按文件名/任务筛选）
     */
    @SaCheckPermission(PermissionConst.MEDIA_READ)
    @GetMapping("/list")
    public MultiResult<MediaFile> list(MediaQueryRequest req) {
        return mediaService.listFiles(req);
    }

    /**
     * 删除媒体记录
     */
    @SaCheckPermission(PermissionConst.MEDIA_DELETE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(mediaService.delete(id));
    }
}
