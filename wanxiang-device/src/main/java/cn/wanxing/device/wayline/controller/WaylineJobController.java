package cn.wanxing.device.wayline.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.device.wayline.dto.WaylineJobCreateRequest;
import cn.wanxing.device.wayline.entity.WaylineJob;
import cn.wanxing.device.wayline.service.WaylineJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 航线任务接口：创建（立即/定时）、列表、取消。
 * 执行进度经 events: flighttask_progress 推送到 /topic/device/{sn}/wayline/progress。
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class WaylineJobController {

    private final WaylineJobService waylineJobService;

    /**
     * 创建航线任务（立即任务同步下发 prepare；定时任务由调度器到点下发）
     */
    @SaCheckPermission(PermissionConst.TASK_CREATE)
    @PostMapping("/{sn}/wayline-job")
    public Result<WaylineJob> create(@PathVariable String sn, @Valid @RequestBody WaylineJobCreateRequest req) {
        return Result.success(waylineJobService.createJob(sn, req));
    }

    /**
     * 航线任务列表（分页维度：按设备/状态筛选，机构隔离）
     */
    @SaCheckPermission(PermissionConst.TASK_READ)
    @GetMapping("/wayline-job/list")
    public Result<List<WaylineJob>> list(@RequestParam(required = false) String deviceSn,
                                         @RequestParam(required = false) String status) {
        return Result.success(waylineJobService.listJobs(deviceSn, status));
    }

    /**
     * 取消任务（仅未执行的任务可取消：pending 本地取消 / sent 下发 undo）
     */
    @SaCheckPermission(PermissionConst.TASK_CONTROL)
    @DeleteMapping("/wayline-job/{id}")
    public Result<Boolean> cancel(@PathVariable Long id) {
        return Result.success(waylineJobService.cancel(id));
    }
}
