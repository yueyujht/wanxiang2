package cn.wanxing.device.airsense.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * AirSense 空域告警消息（thing/product/{sn}/events，method=airsense_warning）：
 * 机场 ADS-B 检测到周边民航飞机时推送，data 为航班数组。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirsenseWarningMessage {

    private String tid;

    private String bid;

    private Long timestamp;

    private String method;

    /** 是否需要云端回执：1 需要（回 events_reply），缺省 0 */
    private Integer needReply;

    /** 检测到的周边航班列表 */
    private List<AirsenseAircraft> data;
}
