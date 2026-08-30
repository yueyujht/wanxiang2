package cn.wanxing.device.wayline.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建航线任务请求（条件任务与断点续飞为二期，字段暂不开放）
 */
@Getter
@Setter
public class WaylineJobCreateRequest {

    /** 任务名称 */
    @Size(max = 128, message = "任务名称最长 128 字符")
    private String name;

    /** 航线文件 ID（sys_wayline_file） */
    @NotNull(message = "航线文件不能为空")
    private Long waylineFileId;

    /** 任务类型：0 立即 / 1 定时（条件任务二期） */
    @NotNull(message = "任务类型不能为空")
    @Min(value = 0, message = "任务类型非法")
    @Max(value = 1, message = "任务类型仅支持立即/定时")
    private Integer taskType;

    /** 执行时间（毫秒时间戳），立即/定时任务必填 */
    @NotNull(message = "执行时间不能为空")
    private Long executeTime;

    /** 返航高度（米，20-1500） */
    @NotNull(message = "返航高度不能为空")
    @Min(value = 20, message = "返航高度最小 20 米")
    @Max(value = 1500, message = "返航高度最大 1500 米")
    private Integer rthAltitude;

    /** 航线失控动作：0 继续执行 / 1 退出并执行失控动作，缺省 0 */
    @Min(value = 0, message = "航线失控动作非法")
    @Max(value = 1, message = "航线失控动作非法")
    private Integer exitWaylineWhenRcLost;

    /** 航线精度：0 GPS / 1 RTK，缺省 1（官方建议默认） */
    @Min(value = 0, message = "航线精度非法")
    @Max(value = 1, message = "航线精度非法")
    private Integer waylinePrecisionType;

    /** 是否模拟器执行（室内调试，不实际起飞），缺省 false */
    private Boolean simulateMission;

    /** 模拟器起始纬度（simulateMission=true 时必填） */
    private Double simulateLatitude;

    /** 模拟器起始经度（simulateMission=true 时必填） */
    private Double simulateLongitude;

    /** 执行条件：最低存储容量（MB），可选 */
    @Min(value = 0, message = "存储容量非法")
    private Integer storageCapacity;
}
