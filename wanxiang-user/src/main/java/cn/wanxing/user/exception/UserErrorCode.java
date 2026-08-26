package cn.wanxing.user.exception;

import cn.wanxing.common.exception.ErrorCode;

/**
 * 用户模块错误码
 */
public enum UserErrorCode implements ErrorCode {
    PHONE_EXISTS("PHONE_EXISTS", "手机号已存在"),
    PHONE_NOT_REGISTERED("PHONE_NOT_REGISTERED", "手机号未录入"),
    CODE_INVALID("CODE_INVALID", "验证码错误或已过期"),
    LOGIN_FAILED("LOGIN_FAILED", "手机号或验证码错误"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账号已被禁用"),
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "角色不存在"),
    ROLE_DISABLED("ROLE_DISABLED", "角色已被停用"),
    ORG_NOT_FOUND("ORG_NOT_FOUND", "机构不存在"),
    ORG_NAME_EXISTS("ORG_NAME_EXISTS", "机构名称已存在"),
    ORG_CODE_EXISTS("ORG_CODE_EXISTS", "机构编码已存在"),
    ORG_DISABLED("ORG_DISABLED", "机构已被停用"),
    OPERATION_FORBIDDEN("OPERATION_FORBIDDEN", "无权执行该操作"),
    CANNOT_OPERATE_SELF("CANNOT_OPERATE_SELF", "不能对自己执行该操作"),
    UPDATE_FAILED("UPDATE_FAILED","更新用户信息失败"),
    INSERT_FAILED("INSERT_FAILED","新增用户失败");

    private final String code;

    private final String message;

    UserErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
