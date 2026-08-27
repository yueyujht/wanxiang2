package cn.wanxing.device.alarm.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 健康告警（HMS）消息信封：thing/product/{sn}/events 主题，method=hms
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HmsMessage {

    private String tid;

    private String bid;

    private Long timestamp;

    private String method;

    private HmsData data;
}