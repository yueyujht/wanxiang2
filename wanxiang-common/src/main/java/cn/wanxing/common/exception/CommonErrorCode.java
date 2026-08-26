package cn.wanxing.common.exception;

/**
 * 通用错误码
 */
public enum CommonErrorCode implements ErrorCode {

    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 非法参数
     */
    ILLEGAL_ARGUMENT("ILLEGAL_ARGUMENT", "非法参数"),

    /**
     * 未登录或登录已失效
     */
    UNAUTHORIZED("UNAUTHORIZED", "未登录或登录已失效"),

    /**
     * 无权限访问
     */
    FORBIDDEN("FORBIDDEN", "无权限访问"),

    /**
     * 系统错误
     */
    SYSTEM_ERROR("SYSTEM_ERROR", "系统错误");

    private String code;

    private String message;

    CommonErrorCode(String code, String message) {
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