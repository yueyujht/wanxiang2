package cn.wanxing.device.constant;

import java.util.Arrays;

/**
 * 设备型号字典：domain + type + sub_type 唯一确定一款设备，映射到型号名称。
 *
 * <p>数据来源：DJI 上云 API 官方「产品支持」文档 + 官方 demo 的 device_dictionary 表。
 * 如需支持更多型号（负载相机等），在此枚举中追加即可。
 */
public enum DeviceModelEnum {

    // 机场（domain=3）
    DOCK(3, 1, 0, "DJI Dock"),
    DOCK_2(3, 2, 0, "DJI Dock 2"),

    // 无人机（domain=0）
    M300(0, 60, 0, "Matrice 300 RTK"),
    M30(0, 67, 0, "Matrice 30"),
    M30T(0, 67, 1, "Matrice 30T"),
    M3E(0, 77, 0, "Mavic 3 Enterprise"),
    M3T(0, 77, 1, "Mavic 3 Thermal"),
    M3M(0, 77, 2, "Mavic 3 Multispectral"),
    M350(0, 89, 0, "Matrice 350 RTK"),
    M3D(0, 91, 0, "Matrice 3D"),
    M3TD(0, 91, 1, "Matrice 3TD"),

    // 遥控器（domain=2）
    RC(2, 56, 0, "DJI Smart Controller"),
    RC_PLUS(2, 119, 0, "DJI RC Plus"),
    RC_PRO(2, 144, 0, "DJI RC Pro"),
    ;

    private final int domain;

    private final int type;

    private final int subType;

    private final String name;

    DeviceModelEnum(int domain, int type, int subType, String name) {
        this.domain = domain;
        this.type = type;
        this.subType = subType;
        this.name = name;
    }

    public int getDomain() {
        return domain;
    }

    public int getType() {
        return type;
    }

    public int getSubType() {
        return subType;
    }

    public String getName() {
        return name;
    }

    /**
     * 按 domain + type + sub_type 解析型号名称；未收录返回 null
     */
    public static String resolveName(Integer domain, Integer type, Integer subType) {
        if (domain == null || type == null || subType == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(model -> model.domain == domain && model.type == type && model.subType == subType)
                .findAny()
                .map(DeviceModelEnum::getName)
                .orElse(null);
    }
}
