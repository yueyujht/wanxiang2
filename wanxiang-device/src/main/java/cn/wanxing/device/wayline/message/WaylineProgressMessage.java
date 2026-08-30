package cn.wanxing.device.wayline.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 航线任务进度消息（thing/product/{sn}/events，method=flighttask_progress）：
 * 任务执行期间设备持续上报，含机场工作流步骤、百分比、断点信息（断点续飞用）与媒体数量。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WaylineProgressMessage {

    private String tid;

    private String bid;

    private Long timestamp;

    private String method;

    /** 是否需要云端回执：1 需要（回 events_reply），缺省 0 */
    private Integer needReply;

    /** 进度数据 */
    private ProgressData data;

    /** 进度数据 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgressData {

        /** 任务执行输出 */
        private Output output;

        /** 协议信封内的返回码（示例中为 0） */
        private Integer result;
    }

    /** 任务执行输出 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Output {

        /** 任务状态：sent/rejected/in_progress/ok/failed/canceled/timeout/partially_done/paused */
        private String status;

        /** 机场工作流进度 */
        private Progress progress;

        /** 扩展信息 */
        private Ext ext;
    }

    /** 机场工作流进度 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Progress {

        /** 执行步骤（0-49 官方枚举：下载 KMZ/上传飞行器/起飞/返航/上传日志等） */
        private Integer currentStep;

        /** 进度百分比 0-100 */
        private Integer percent;
    }

    /** 扩展信息 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ext {

        /** 任务 ID（与平台 flight_id 一致） */
        private String flightId;

        /** 当前执行到的航点数 */
        private Integer currentWaypointIndex;

        /** 航线任务状态（飞行器侧，0-9 官方枚举） */
        private Integer waylineMissionState;

        /** 当前作业的航线 ID */
        private Integer waylineId;

        /** 本次任务产生的媒体文件数量 */
        private Integer mediaCount;

        /** 航迹 ID */
        private String trackId;

        /** 断点信息（任务中断时上报，断点续飞依据） */
        private BreakPoint breakPoint;
    }

    /** 断点信息 */
    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BreakPoint {

        /** 断点序号 */
        private Integer index;

        /** 断点状态：0 在航段上 / 1 在航点上 */
        private Integer state;

        /** 当前航段进度 0-1 */
        private Double progress;

        /** 航线 ID */
        private Integer waylineId;

        /** 中断原因（官方长枚举） */
        private Integer breakReason;

        /** 断点纬度 */
        private Double latitude;

        /** 断点经度 */
        private Double longitude;

        /** 断点相对椭球面高度（米） */
        private Double height;

        /** 断点偏航轴角度 */
        private Double attitudeHead;
    }
}
