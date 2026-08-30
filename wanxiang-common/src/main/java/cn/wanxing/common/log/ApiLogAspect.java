package cn.wanxing.common.log;

import cn.wanxing.common.exception.BizException;
import cn.wanxing.common.exception.CommonErrorCode;
import cn.wanxing.common.utils.BeanValidator;
import com.alibaba.fastjson2.JSON;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


/**
 * 接口日志切面：拦截标注了 {@link ApiLog} 的方法（Service 层业务方法），做入参校验 + 记录入参、耗时、结果/异常。
 *
 * <p>入参校验统一在这里做，业务代码里无需再写 {@code @Valid} 校验。
 * 校验失败抛 {@link BizException}，由全局异常处理器统一返回错误响应。
 *
 * <p>每次调用记一条完成日志（含耗时与结果），异常时另记一条 ERROR；请求级的访问日志
 * （URI/状态码/用户/IP）由 {@link HttpRequestLogFilter} 统一记录，两者通过 traceId 关联。
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    /** 日志单字段最大长度，超出截断，避免大响应刷屏 */
    private static final int MAX_LOG_LENGTH = 1000;

    @Around("@annotation(apiLog)")
    public Object around(ProceedingJoinPoint pjp, ApiLog apiLog) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String name = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        Object[] args = pjp.getArgs();

        //循环遍历所有参数，进行参数校验；null 参数跳过（可空请求体的约定，如 @RequestBody(required=false)），
        // 且 Hibernate Validator 对 null 直接抛 IllegalArgumentException，跳过避免把空请求体变成 500
        for (Object parameter : args) {
            if (parameter == null) {
                continue;
            }
            try {
                BeanValidator.validateObject(parameter);
            } catch (ValidationException e) {
                log.warn("[API] 参数校验失败 {} desc={} error={}", name, apiLog.value(), e.getMessage());
                throw new BizException(e.getMessage(), CommonErrorCode.ILLEGAL_ARGUMENT);
            }
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object result = pjp.proceed();
            log.info("[API] 完成 {} desc={} cost={}ms args={} result={}",
                    name, apiLog.value(), stopWatch.getTime(), toJson(args), toJson(result));
            return result;
        } catch (Throwable t) {
            log.error("[API] 异常 {} desc={} cost={}ms args={} error={}",
                    name, apiLog.value(), stopWatch.getTime(), toJson(args), t.getMessage(), t);
            // 重新抛出，交给全局异常处理器统一响应
            throw t;
        }
    }

    /**
     * 对象转 JSON 字符串（失败或超长时兜底/截断）
     */
    private String toJson(Object obj) {
        try {
            String json = JSON.toJSONString(obj);
            if (json != null && json.length() > MAX_LOG_LENGTH) {
                json = json.substring(0, MAX_LOG_LENGTH) + "...(截断)";
            }
            return json;
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
