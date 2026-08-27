package cn.wanxing.device.alarm.dto;

import cn.wanxing.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 告警列表查询请求（分页 + 筛选）
 */
@Getter
@Setter
public class AlarmQueryRequest extends PageRequest {

    /** 设备 SN 筛选 */
    private String deviceSn;

    /** 告警等级筛选：0 通知 / 1 提醒 / 2 警告 */
    private Integer level;
}