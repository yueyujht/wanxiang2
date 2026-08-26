package cn.wanxing.device.dto.vo;

import cn.wanxing.device.constant.DevicePropertyEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 可设置设备属性 schema（供前端渲染设置界面）
 */
@Getter
@Setter
public class DevicePropertySchemaVO {

    /** 属性名 */
    private String property;

    /** 中文名 */
    private String name;

    /** 值类型：bool / enum_int */
    private String type;

    /** 枚举值 → 说明（bool 类型为 null，前端渲染开关即可） */
    private Map<Integer, String> enumValues;

    public static DevicePropertySchemaVO from(DevicePropertyEnum p) {
        DevicePropertySchemaVO vo = new DevicePropertySchemaVO();
        vo.property = p.getName();
        vo.name = p.getDisplayName();
        vo.type = p.getType().name().toLowerCase();
        vo.enumValues = p.getEnumValues();
        return vo;
    }
}