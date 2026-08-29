package cn.wanxing.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.wanxing.common.exception.BizException;
import cn.wanxing.common.exception.CommonErrorCode;
import cn.wanxing.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：统一转成 {@link Result} 错误响应。
 *
 * <p>每个分支都记录日志（业务类问题 WARN，未知系统异常 ERROR），日志自动携带 traceId，
 * 前端/调用方可凭响应头 X-Trace-Id 反馈问题精确定位。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBusiness(BizException e) {
        // 业务异常是预期内行为，不打印堆栈
        log.warn("业务异常 code={} message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.error(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", firstFieldError(e.getBindingResult().getFieldError()));
        return Result.error(CommonErrorCode.ILLEGAL_ARGUMENT.getCode(), firstFieldError(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        log.warn("参数绑定失败: {}", firstFieldError(e.getBindingResult().getFieldError()));
        return Result.error(CommonErrorCode.ILLEGAL_ARGUMENT.getCode(), firstFieldError(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("参数约束失败: {}", e.getMessage());
        return Result.error(CommonErrorCode.ILLEGAL_ARGUMENT.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public Result<Void> handleIllegalArgument(Exception e) {
        log.warn("参数不合法: {}", e.getMessage());
        return Result.error(CommonErrorCode.ILLEGAL_ARGUMENT.getCode(), "参数不合法");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体无法解析: {}", e.getMessage());
        return Result.error(CommonErrorCode.ILLEGAL_ARGUMENT.getCode(), "请求体无法解析");
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录或登录已失效: {}", e.getMessage());
        return Result.error(CommonErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public Result<Void> handleNoAuth(Exception e) {
        log.warn("无权限访问: {}", e.getMessage());
        return Result.error(CommonErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error(CommonErrorCode.SYSTEM_ERROR);
    }

    private String firstFieldError(FieldError fieldError) {
        return fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
    }
}
