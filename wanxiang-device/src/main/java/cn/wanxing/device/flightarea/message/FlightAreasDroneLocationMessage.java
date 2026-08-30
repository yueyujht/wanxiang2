package cn.wanxing.device.flightarea.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 自定义飞行区告警消息（thing/product/{sn}/events，method=flight_areas_drone_location）：
 * 飞行中飞行器对各自定义飞行区的距离/进出状态推送。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightAreasDroneLocationMessage {

    private String tid;

    private String bid;

    private Long timestamp;

    private String method;

    /** 告警数据 */
    private DroneLocationsData data;

    /** 告警数据 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DroneLocationsData {

        /** 飞行器与各自定义飞行区的位置关系 */
        private List<DroneLocation> droneLocations;
    }

    /** 单个区域的飞行器位置关系 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DroneLocation {

        /** 区域唯一 ID（飞行区文件中的 area_id） */
        private String areaId;

        /** 距飞行边界的距离（米） */
        private Double areaDistance;

        /** 是否在自定义飞行区内 */
        private Boolean isInArea;
    }
}
