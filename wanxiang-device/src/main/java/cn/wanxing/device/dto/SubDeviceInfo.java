package cn.wanxing.device.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 拓扑消息中的子设备（如机场内的无人机）
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubDeviceInfo {

    /** 子设备序列号 */
    private String sn;

    /** 设备域（0 飞行器 / 1 负载 / 2 遥控器 / 3 机场） */
    private Integer domain;

    /** 设备型号 */
    private Integer type;

    /** 设备子型号 */
    private Integer subType;

    /** 负载挂载位置索引（如 A/B） */
    private String index;
}