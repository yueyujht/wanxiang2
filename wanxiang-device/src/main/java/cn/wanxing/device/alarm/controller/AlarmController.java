package cn.wanxing.device.alarm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.alarm.dto.AlarmQueryRequest;
import cn.wanxing.device.alarm.service.AlarmService;
import cn.wanxing.device.alarm.entity.Alarm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警查询接口
 */
@RestController
@RequestMapping("/device/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService deviceEventService;

    /**
     * 告警列表（分页 + 按设备/等级筛选）
     */
    @SaCheckPermission(PermissionConst.ALARM_READ)
    @GetMapping("/list")
    public MultiResult<Alarm> list(AlarmQueryRequest req) {
        return deviceEventService.listAlarms(req);
    }
}