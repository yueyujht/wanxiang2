package cn.wanxing.device.device.entity;

import cn.wanxing.device.device.constant.DeviceStatusEnum;
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

    /** 设备索引（遥控器 A控/B控） */
    private String deviceIndex;

    /** 设备密钥（鉴权用） */
    private String deviceSecret;

    /** 一次性随机数（鉴权用） */
    private String nonce;

    /** 物模型版本 */
    private String thingVersion;

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
        return create(sn, name, orgId, modelKeys, modelName, null, null, null, null, false);
    }

    public static Device create(String sn, String name, Long orgId, int[] modelKeys, String modelName,
                                String parentSn, String deviceSecret, String nonce, String thingVersion, Boolean online) {
        Device device = new Device();
        device.setSn(sn);
        device.setName(name);
        device.setOrgId(orgId);
        device.setDomain(modelKeys[0]);
        device.setType(modelKeys[1]);
        device.setSubType(modelKeys[2]);
        device.setModelName(modelName);
        device.setParentSn(parentSn);
        device.setDeviceSecret(deviceSecret);
        device.setNonce(nonce);
        device.setThingVersion(thingVersion);
        device.setStatus(DeviceStatusEnum.OFFLINE);
        if(online){
            device.setStatus(DeviceStatusEnum.ONLINE);
            device.setLastOnlineAt(LocalDateTime.now());
        }else {
            device.setStatus(DeviceStatusEnum.OFFLINE);
        }
        return device;
    }

    // 更新解除绑定的旧设备（重新绑定）
    public static void updateForRebind(Device device, Long orgId, String name) {
        device.setOrgId(orgId);
        device.setName(name);
        device.setStatus(DeviceStatusEnum.OFFLINE);
        device.setBoundAt(LocalDateTime.now());
    }

    // 更新设备上下线信息
    public static void updateForTopo(Device device, Boolean online) {
        updateForTopo(device, null, null, null, online);
    }

    public static void updateForTopo(Device device, String deviceSecret, String nonce, String thingVersion, Boolean online) {
        if(deviceSecret != null && !deviceSecret.isEmpty() && !deviceSecret.equals(device.getDeviceSecret())) device.setDeviceSecret(deviceSecret);
        if(nonce != null && !nonce.isEmpty() && !nonce.equals(device.getNonce())) device.setNonce(nonce);
        if(thingVersion != null && !thingVersion.isEmpty() && !thingVersion.equals(device.getThingVersion())) device.setThingVersion(thingVersion);
        if(online){
            device.setStatus(DeviceStatusEnum.ONLINE);
            device.setLastOnlineAt(LocalDateTime.now());
        }else {
            device.setStatus(DeviceStatusEnum.OFFLINE);
        }
    }
}