package cn.wanxing.user.constant;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 通用启用/停用状态（值 = 枚举名）
 */
public enum EnableStatusEnum implements IEnum<String> {

    /** 启用 */
    ENABLED,

    /** 停用 */
    DISABLED;

    @Override
    public String getValue() {
        return name();
    }
}
