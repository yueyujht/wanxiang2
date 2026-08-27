package cn.wanxing.device.device.dto;

import cn.wanxing.device.device.entity.Device;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 设备信息 VO
 */
@Getter
@Setter
public class DeviceVO {

    private Long id;

    /** 设备序列号 */
    private String sn;

    /** 设备名称 */
    private String name;

    /** 所属机构 ID（未绑定为 null） */
    private Long orgId;

    /** 机构名称 */
    private String orgName;

    /** 父设备 SN（如无人机所属机场） */
    private String parentSn;

    /** 设备域 0 无人机 / 1 负载 / 2 遥控器 / 3 机场 */
    private Integer domain;

    /** 设备型号 */
    private Integer type;

    /** 设备子型号 */
    private Integer subType;

    /** 设备型号名称 */
    private String modelName;

    /** 设备索引（遥控器 A控/B控） */
    private String deviceIndex;

    /** ONLINE / OFFLINE */
    private String status;

    private LocalDateTime lastOnlineAt;

    /** 绑定组织时间 */
    private LocalDateTime boundAt;

    private LocalDateTime createdAt;

    public static DeviceVO from(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.id = device.getId();
        vo.sn = device.getSn();
        vo.name = device.getName();
        vo.orgId = device.getOrgId();
        vo.parentSn = device.getParentSn();
        vo.domain = device.getDomain();
        vo.type = device.getType();
        vo.subType = device.getSubType();
        vo.modelName = device.getModelName();
        vo.deviceIndex = device.getDeviceIndex();
        vo.status = device.getStatus() != null ? device.getStatus().name() : null;
        vo.lastOnlineAt = device.getLastOnlineAt();
        vo.boundAt = device.getBoundAt();
        vo.createdAt = device.getCreatedAt();
        return vo;
    }
}