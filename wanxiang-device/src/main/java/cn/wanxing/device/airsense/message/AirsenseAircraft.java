package cn.wanxing.device.airsense.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * ADS-B 检测到的单架周边航班信息
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirsenseAircraft {

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
}
