package cn.wanxing.common.result;

import cn.wanxing.common.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

import static cn.wanxing.common.exception.CommonErrorCode.SUCCESS;


/**
 * 返回结果
 */
@Getter
@Setter
public class Result<T> {
    /**
     * 状态码
     */
    private String code;

    /**
     * 消息描述
     */
    private String message;

    /**
     * 数据，可以是任何类型的VO
     */
    private T data;

    public Result() {
    }

    public Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS.getCode(), SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(String errorCode, String errorMsg) {
        return new Result<>(errorCode, errorMsg, null);
    }
}