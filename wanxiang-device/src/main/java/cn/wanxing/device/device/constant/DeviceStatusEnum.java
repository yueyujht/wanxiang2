package cn.wanxing.device.device.constant;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 设备在线状态（值 = 枚举名）
 */
public enum DeviceStatusEnum implements IEnum<String> {

    /** 在线 */
    ONLINE,

    /** 离线 */
    OFFLINE;

    @Override
    public String getValue() {
        return name();
    }
}