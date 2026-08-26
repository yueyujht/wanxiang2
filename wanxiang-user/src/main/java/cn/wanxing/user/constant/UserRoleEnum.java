package cn.wanxing.user.constant;

/**
 * 预定义角色（值 = 枚举名）
 */
public enum UserRoleEnum {

    /** 平台超管 */
    SUPER_ADMIN,

    /** 机构管理员 */
    ADMIN,

    /** 操作员 */
    OPERATOR,

    /** 观察员 */
    OBSERVER;

    /**
     * 是否为平台超管角色
     */
    public static boolean isSuperAdmin(String code) {
        return SUPER_ADMIN.name().equals(code);
    }
}
