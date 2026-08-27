package cn.wanxing.device.alarm.entity;

import cn.wanxing.device.alarm.message.HmsAlarm;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 告警实体（sys_alarm）
 */
@Getter
@Setter
@TableName("sys_alarm")
public class Alarm {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上报该告警的设备 SN */
    private String deviceSn;

    /** 告警等级：0 通知 / 1 提醒 / 2 警告 */
    private Integer level;

    /** 事件模块：0 飞行任务 / 1 设备管理 / 2 媒体 / 3 hms */
    private Integer module;

    /** 是否飞行：0 在地上 / 1 在天上 */
    private Integer inTheSky;

    /** 告警码，如 0x16100083 */
    private String code;

    /** 设备类型 {domain-type-subtype}，如 0-67-0 */
    private String deviceType;

    /** 是否及时性告警：0 否 / 1 是 */
    private Integer imminent;

    /** 参数原文（JSON） */
    private String args;

    /** 填充后的告警文案（中文） */
    private String message;

    /** 告警时间 */
    private LocalDateTime createdAt;

    /**
     * 由 HMS 告警消息构建告警实体（文案由外部注入的 {@link HmsDictionary} 填充）
     */
    public static Alarm create(HmsAlarm hmsAlarm, String sn, HmsDictionary hmsDictionary) {
        Alarm alarm = new Alarm();
        alarm.setDeviceSn(sn);
        alarm.setLevel(hmsAlarm.getLevel());
        alarm.setModule(hmsAlarm.getModule());
        alarm.setInTheSky(hmsAlarm.getInTheSky());
        alarm.setCode(hmsAlarm.getCode());
        alarm.setDeviceType(hmsAlarm.getDeviceType());
        alarm.setImminent(hmsAlarm.getImminent());
        alarm.setArgs(hmsAlarm.getArgs() == null ? null : hmsAlarm.getArgs().toString());
        alarm.setMessage(hmsDictionary.resolveMessage(hmsAlarm.getDeviceType(), hmsAlarm.getCode(),
                hmsAlarm.getInTheSky(), hmsAlarm.getArgs()));
        alarm.setCreatedAt(LocalDateTime.now());
        return alarm;
    }
}