package cn.wanxing.device.device.constant;

import java.util.Arrays;

/**
 * 设备型号字典：domain + type + sub_type 唯一确定一款设备，映射到型号名称。
 *
 * <p>数据来源：DJI 上云 API 官方「产品支持」文档
 * （https://developer.dji.com/doc/cloud-api-tutorial/cn/overview/product-support.html）
 * 的「设备枚举值」表，覆盖机场（domain=3）、无人机（domain=0）、遥控器（domain=2）。
 * 如需支持更多型号，在此枚举中追加即可。
 *
 * <p>注意：负载/相机（domain=1）不在此枚举中，因为负载由 type + sub_type + gimbalindex
 * 三维唯一确定（如禅思 Z30 三个云台口分别为 20-0-0 / 20-0-1 / 20-0-2），需要额外的 gimbalindex
 * 维度，与设备维度分开处理。
 */
public enum DeviceModelEnum {

    // 机场（domain=3）
    DOCK(3, 1, 0, "DJI Dock"),
    DOCK_2(3, 2, 0, "DJI Dock 2"),
    DOCK_3(3, 3, 0, "DJI Dock 3"),

    // 无人机（domain=0）
    M400(0, 103, 0, "Matrice 400"),
    M350(0, 89, 0, "Matrice 350 RTK"),
    M300(0, 60, 0, "Matrice 300 RTK"),
    M30(0, 67, 0, "Matrice 30"),
    M30T(0, 67, 1, "Matrice 30T"),
    M3E(0, 77, 0, "Mavic 3E"),
    M3T(0, 77, 1, "Mavic 3T"),
    M3TA(0, 77, 3, "Mavic 3TA"),
    M3D(0, 91, 0, "Matrice 3D"),
    M3TD(0, 91, 1, "Matrice 3TD"),
    M4D(0, 100, 0, "Matrice 4D"),
    M4TD(0, 100, 1, "Matrice 4TD"),
    M4E(0, 99, 0, "Matrice 4E"),
    M4T(0, 99, 1, "Matrice 4T"),

    // 遥控器（domain=2）
    RC(2, 56, 0, "DJI Smart Controller Enterprise"),
    RC_PLUS(2, 119, 0, "DJI RC Plus"),
    RC_PLUS_2(2, 174, 0, "DJI RC Plus 2"),
    RC_PRO(2, 144, 0, "DJI RC Pro Enterprise"),

    // 未知设备
    UNKNOWN(-1, -1, -1, "Unknown");

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
     * 按 domain + type + sub_type 解析型号名称
     */
    public static String resolveName(Integer domain, Integer type, Integer subType) {
        if (domain == null || type == null || subType == null) {
            return UNKNOWN.getName();
        }

        return Arrays.stream(values())
                .filter(model -> model.domain == domain && model.type == type && model.subType == subType)
                .findAny()
                .map(DeviceModelEnum::getName)
                .orElse(null);
    }
}