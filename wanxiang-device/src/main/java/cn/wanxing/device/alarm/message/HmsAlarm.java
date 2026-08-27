package cn.wanxing.device.alarm.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 单条健康告警
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HmsAlarm {

    /** 告警等级：0 通知 / 1 提醒 / 2 警告 */
    private Integer level;

    /** 事件模块：0 飞行任务 / 1 设备管理 / 2 媒体 / 3 hms */
    private Integer module;

    /** 是否飞行：0 在地上 / 1 在天上 */
    private Integer inTheSky;

    /** 告警码，如 0x16100083 */
    private String code;

    /** 设备类型，格式 {domain-type-subtype}，如 0-67-0 */
    private String deviceType;

    /** 是否及时性告警：0 否 / 1 是 */
    private Integer imminent;

    /** 参数（sensor_index、component_index 等，用于填充文案） */
    private JsonNode args;
}