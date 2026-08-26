package cn.wanxing.device.entity;

import cn.wanxing.device.constant.DeviceStatusEnum;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 设备实体（sys_device）
 */
@Getter
@Setter
@TableName("sys_device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备序列号（唯一） */
    private String sn;

    /** 设备名称（绑定时填写的 device_callsign） */
    private String name;

    /** 所属机构 ID（未绑定为 null） */
    private Long orgId;

    /** 父设备 SN（如无人机所属机场） */
    private String parentSn;

    /** 设备域 0 无人机 / 1 负载 / 2 遥控器 / 3 机场 */
    private Integer domain;

    /** 设备型号 */
    private Integer type;

    /** 设备子型号 */
    private Integer subType;

    /** 设备型号名称（如 DJI Dock / Matrice 30） */
    private String modelName;

    /** 固件版本（来自 state 消息） */
    private String firmwareVersion;

    /** 设备索引（遥控器 A控/B控） */
    private String deviceIndex;

    /** 在线状态 */
    private DeviceStatusEnum status;

    /** 最近上线时间 */
    private LocalDateTime lastOnlineAt;

    /** 绑定组织时间（解绑清空） */
    private LocalDateTime boundAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 创建新设备（绑定）
    public static Device create(String sn, String name, Long orgId, int[] modelKeys, String modelName) {
        Device device = new Device();
        device.setSn(sn);
        device.setName(name);
        device.setOrgId(orgId);
        device.setDomain(modelKeys[0]);
        device.setType(modelKeys[1]);
        device.setSubType(modelKeys[2]);
        device.setModelName(modelName);
        device.setStatus(DeviceStatusEnum.OFFLINE);
        device.setBoundAt(LocalDateTime.now());
        return device;
    }

    // 更新解除绑定的旧设备（重新绑定）
    public static Device update(Device device, Long orgId, String name) {
        device.setOrgId(orgId);
        device.setName(name);
        device.setStatus(DeviceStatusEnum.OFFLINE);
        device.setBoundAt(LocalDateTime.now());
        return device;
    }
}