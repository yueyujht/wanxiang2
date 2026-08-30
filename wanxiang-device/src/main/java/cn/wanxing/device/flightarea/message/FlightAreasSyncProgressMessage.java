package cn.wanxing.device.flightarea.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 自定义飞行区文件同步进度消息（thing/product/{sn}/events，method=flight_areas_sync_progress）：
 * 机场把飞行区文件同步到飞行器的进展回报。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightAreasSyncProgressMessage {

    private String tid;

    private String bid;

    private Long timestamp;

    private String method;

    /** 是否需要云端回执：1 需要（回 events_reply），缺省 0 */
    private Integer needReply;

    /** 同步进度数据 */
    private SyncProgressData data;

    /** 同步进度数据 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SyncProgressData {

        /** 同步状态：wait_sync 待同步 / synchronizing 同步中 / synchronized 已同步 / fail 失败 / switch_fail 使能开关失败 */
        private String status;

        /** 失败原因码（1-13，官方枚举；0 或缺省为无错误） */
        private Integer reason;

        /** 同步的文件信息 */
        private SyncFile file;
    }

    /** 同步的文件信息 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SyncFile {

        /** 文件名 */
        private String name;

        /** 文件 SHA256 签名 */
        private String checksum;
    }
}
