package cn.wanxing.device.wayline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 航线任务实体（sys_wayline_job）：飞行任务生命周期，状态由平台流转（pending/sent/executing）
 * 与设备进度（progress.status：ok/failed/canceled/timeout/partially_done/paused）共同驱动
 */
@Getter
@Setter
@TableName("sys_wayline_job")
public class WaylineJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务 ID（平台生成 UUID，贯穿 prepare/execute/progress/undo/resource_get） */
    private String flightId;

    /** 所属机构 ID（取目标设备的机构） */
    private Long orgId;

    /** 目标网关设备 SN（机场） */
    private String deviceSn;

    /** 航线文件 ID（sys_wayline_file） */
    private Long waylineFileId;

    /** 任务名称 */
    private String name;

    /** 任务类型：0 立即 / 1 定时（2 条件任务二期） */
    private Integer taskType;

    /** 执行时间（毫秒时间戳） */
    private Long executeTime;

    /** 返航高度（米） */
    private Integer rthAltitude;

    /** 返航高度模式：0 智能 / 1 设定（机场仅支持 1） */
    private Integer rthMode;

    /** 失控动作（协议当前固定 0 返航） */
    private Integer outOfControlAction;

    /** 航线失控动作：0 继续执行 / 1 退出并执行失控动作 */
    private Integer exitWaylineWhenRcLost;

    /** 航线精度：0 GPS / 1 RTK */
    private Integer waylinePrecisionType;

    /** 是否模拟器执行（室内调试，不实际起飞） */
    private Boolean simulateMission;

    /** 模拟器起始纬度 */
    private Double simulateLatitude;

    /** 模拟器起始经度 */
    private Double simulateLongitude;

    /** 执行条件：最低存储容量（MB） */
    private Integer storageCapacity;

    /** 任务状态：pending/sent/executing/ok/failed/canceled/timeout/partially_done/paused/rejected */
    private String status;

    /** 平台已下发 flighttask_prepare */
    private Boolean prepareSent;

    /** 平台已下发 flighttask_execute */
    private Boolean executeSent;

    /** 最新执行步骤（机场工作流） */
    private Integer currentStep;

    /** 最新进度百分比（0-100） */
    private Integer percent;

    /** 本次任务产生的媒体文件数量 */
    private Integer mediaCount;

    /** 最新断点信息（progress.ext.break_point 原文 JSON，供二期断点续飞） */
    private String breakpoint;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 创建任务实体（初始 pending），flight_id 由平台生成
     */
    public static WaylineJob create(String sn, Long orgId, Long waylineFileId, String name,
                                    Integer taskType, Long executeTime, WaylineJobParams params) {
        WaylineJob job = new WaylineJob();
        job.setFlightId(UUID.randomUUID().toString());
        job.setDeviceSn(sn);
        job.setOrgId(orgId);
        job.setWaylineFileId(waylineFileId);
        job.setName(name);
        job.setTaskType(taskType);
        job.setExecuteTime(executeTime);
        job.setRthAltitude(params.rthAltitude());
        // 机场仅支持设定高度模式
        job.setRthMode(1);
        // 协议当前固定 0 返航
        job.setOutOfControlAction(0);
        job.setExitWaylineWhenRcLost(params.exitWaylineWhenRcLost() == null ? 0 : params.exitWaylineWhenRcLost());
        job.setWaylinePrecisionType(params.waylinePrecisionType() == null ? 1 : params.waylinePrecisionType());
        job.setSimulateMission(Boolean.TRUE.equals(params.simulateMission()));
        job.setSimulateLatitude(params.simulateLatitude());
        job.setSimulateLongitude(params.simulateLongitude());
        job.setStorageCapacity(params.storageCapacity());
        job.setStatus("pending");
        job.setPrepareSent(false);
        job.setExecuteSent(false);
        return job;
    }

    /** 任务创建参数（与 prepare 下发字段对应，缺省值在工厂方法内收敛） */
    public record WaylineJobParams(Integer rthAltitude, Integer exitWaylineWhenRcLost,
                                   Integer waylinePrecisionType, Boolean simulateMission,
                                   Double simulateLatitude, Double simulateLongitude,
                                   Integer storageCapacity) {
    }

    /**
     * 应用一次进度上报（status/步骤/百分比/媒体数量/断点），字段缺省保持原值
     */
    public void applyProgress(String status, Integer currentStep, Integer percent,
                              Integer mediaCount, String breakpointJson) {
        if (status != null) {
            setStatus(status);
        }
        if (currentStep != null) {
            setCurrentStep(currentStep);
        }
        if (percent != null) {
            setPercent(percent);
        }
        if (mediaCount != null) {
            setMediaCount(mediaCount);
        }
        if (breakpointJson != null) {
            setBreakpoint(breakpointJson);
        }
    }
}
