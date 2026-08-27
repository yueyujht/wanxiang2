package cn.wanxing.device.state.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 设备最新状态实体（sys_device_state）：每台设备只保留最新一条 state。
 */
@Getter
@Setter
@TableName("sys_device_state")
public class DeviceState {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备序列号（唯一） */
    private String deviceSn;

    /** 最近一次 state 消息的 data 部分原文（JSON），兜底存全部字段 */
    private String stateJson;

    /** 固件版本 */
    private String firmwareVersion;

    /** 告警状态（enum_int） */
    private Integer alarmState;

    /** 飞机是否在舱（enum_int） */
    private Integer droneInDock;

    /** 舱盖状态（enum_int） */
    private Integer coverState;

    /** 当前模式码（enum_int） */
    private Integer modeCode;

    /** 固件升级状态（enum_int） */
    private Integer firmwareUpgradeStatus;

    /** 最近一次更新时间 */
    private LocalDateTime updatedAt;

    public static DeviceState update(String sn, JsonNode data) {
        DeviceState deviceState = new DeviceState();
        deviceState.setDeviceSn(sn);
        deviceState.setStateJson(data.toString());
        deviceState.setFirmwareVersion(asText(data, "firmware_version"));
        deviceState.setAlarmState(asInteger(data, "alarm_state"));
        deviceState.setDroneInDock(asInteger(data, "drone_in_dock"));
        deviceState.setCoverState(asInteger(data, "cover_state"));
        deviceState.setModeCode(asInteger(data, "mode_code"));
        deviceState.setFirmwareUpgradeStatus(asInteger(data, "firmware_upgrade_status"));
        return deviceState;
    }

    /**
     * 取 JSON 字段的文本值，字段不存在或为 null 时返回 null
     */
    private static String asText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    /**
     * 取 JSON 字段的整数值，字段不存在或为 null 时返回 null
     */
    private static Integer asInteger(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asInt();
    }

}