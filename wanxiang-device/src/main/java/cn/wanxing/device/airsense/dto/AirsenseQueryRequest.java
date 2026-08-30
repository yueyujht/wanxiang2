package cn.wanxing.device.airsense.dto;

import cn.wanxing.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * AirSense 告警查询条件
 */
@Getter
@Setter
public class AirsenseQueryRequest extends PageRequest {

    /** 设备 SN 筛选 */
    private String deviceSn;

    /** 危险等级筛选：0 无危险 / 1-4 等级一至四 */
    private Integer warningLevel;
}
