package cn.wanxing.user.constant;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 用户账号状态（值 = 枚举名）
 */
public enum UserStateEnum implements IEnum<String> {

    /** 启用 */
    ENABLED,

    /** 禁用 */
    DISABLED;

    @Override
    public String getValue() {
        return name();
    }
}
