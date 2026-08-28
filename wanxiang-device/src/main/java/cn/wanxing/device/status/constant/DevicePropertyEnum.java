package cn.wanxing.device.status.constant;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Map;

/**
 * 可下发（accessMode = rw / w）的设备属性字典。
 * <p>Dock 3 可下发的属性只有三个，其余属性 accessMode=r 为只读，由 state/osd 消息上报，不在本字典中：
 * <ul>
 *   <li>air_transfer_enable —— 空中回传（bool）</li>
 *   <li>silent_mode —— 机场静音模式（enum_int 0/1）</li>
 *   <li>user_experience_improvement —— 用户体验改善计划（enum_int 0/1/2）</li>
 * </ul>
 * 后续支持无人机、遥控器等其它设备型号的可下发属性时，在此追加即可。
 */
public enum DevicePropertyEnum {

    AIR_TRANSFER_ENABLE("air_transfer_enable", "空中回传", PropertyType.BOOL, null),
    SILENT_MODE("silent_mode", "机场静音模式", PropertyType.ENUM_INT,
            Map.of(0, "非静音模式", 1, "静音模式")),
    USER_EXPERIENCE_IMPROVEMENT("user_experience_improvement", "用户体验改善计划", PropertyType.ENUM_INT,
            Map.of(0, "初始状态", 1, "拒绝加入用户体验改善计划", 2, "同意加入用户体验改善计划"));

    /** 属性值类型 */
    public enum PropertyType {
        BOOL,
        ENUM_INT
    }

    /** 属性名（property/set 消息 data 中的 key） */
    private final String name;

    /** 中文名 */
    private final String displayName;

    /** 值类型 */
    private final PropertyType type;

    /** 枚举值 → 说明（仅 ENUM_INT 使用，bool 类型为 null） */
    private final Map<Integer, String> enumValues;

    DevicePropertyEnum(String name, String displayName, PropertyType type, Map<Integer, String> enumValues) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.enumValues = enumValues;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PropertyType getType() {
        return type;
    }

    public Map<Integer, String> getEnumValues() {
        return enumValues;
    }

    /**
     * 按属性名查字典，找不到返回 null
     */
    public static DevicePropertyEnum of(String name) {
        if (name == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(p -> p.name.equals(name))
                .findAny()
                .orElse(null);
    }

    /**
     * 校验属性值是否符合类型 / 值域，合法返回 true
     */
    public boolean isValid(JsonNode value) {
        if (value == null) {
            return false;
        }
        return switch (type) {
            case BOOL -> value.isBoolean();
            case ENUM_INT -> value.isInt() && enumValues.containsKey(value.asInt());
        };
    }
}