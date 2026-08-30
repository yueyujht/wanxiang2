package cn.wanxing.device.airsense.entity;

import cn.wanxing.device.airsense.message.AirsenseAircraft;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AirSense 空域告警实体（sys_airsense_warning）
 */
@Getter
@Setter
@TableName("sys_airsense_warning")
public class AirsenseWarning {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上报的网关设备 SN（机场） */
    private String deviceSn;

    /** ICAO 民用航空飞机地址 */
    private String icao;

    /** 危险等级：0 无危险 / 1-4 等级一至四（>=3 建议无人机避让） */
    private Integer warningLevel;

    /** 纬度（6 位小数） */
    private Double latitude;

    /** 经度（6 位小数） */
    private Double longitude;

    /** 绝对高度（米） */
    private Integer altitude;

    /** 高度类型：0 椭球高 / 1 海拔高 */
    private Integer altitudeType;

    /** 航向（度，1 位小数） */
    private Double heading;

    /** 航班相对无人机高度（米） */
    private Integer relativeAltitude;

    /** 垂直趋势：0 不变 / 1 上升 / 2 下降 */
    private Integer vertTrend;

    /** 航班与无人机的水平距离（米） */
    private Integer distance;

    /** 告警时间 */
    private LocalDateTime createdAt;

    /**
     * 由一条 ADS-B 航班信息构建告警实体
     */
    public static AirsenseWarning create(String sn, AirsenseAircraft aircraft) {
        AirsenseWarning warning = new AirsenseWarning();
        warning.setDeviceSn(sn);
        warning.setIcao(aircraft.getIcao());
        warning.setWarningLevel(aircraft.getWarningLevel());
        warning.setLatitude(aircraft.getLatitude());
        warning.setLongitude(aircraft.getLongitude());
        warning.setAltitude(aircraft.getAltitude());
        warning.setAltitudeType(aircraft.getAltitudeType());
        warning.setHeading(aircraft.getHeading());
        warning.setRelativeAltitude(aircraft.getRelativeAltitude());
        warning.setVertTrend(aircraft.getVertTrend());
        warning.setDistance(aircraft.getDistance());
        warning.setCreatedAt(LocalDateTime.now());
        return warning;
    }
}
