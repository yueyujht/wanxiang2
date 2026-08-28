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
 * 接口日志切面：拦截标注了 {@link ApiLog} 的方法，做入参校验 + 记录入参、耗时、结果/异常。
 *
 * <p>入参校验统一在这里做，业务代码里无需再写 {@code @Valid} 校验。
 * 校验失败抛 {@link BizException}，由全局异常处理器统一返回错误响应。
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    /** 日志单字段最大长度，超出截断，避免大响应刷屏 */
    private static final int MAX_LOG_LENGTH = 1000;

    @Around("@annotation(apiLog)")
    public Object around(ProceedingJoinPoint pjp, ApiLog apiLog) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();

        log.info("[API] 请求开始 method={} desc={} args={}", method.getName(), apiLog.value(), toJson(args));

        //循环遍历所有参数，进行参数校验
        for (Object parameter : args) {
            try {
                BeanValidator.validateObject(parameter);
            } catch (ValidationException e) {
                log.error("[API] 参数异常 method={} cost={}ms error={}", method, stopWatch.getTime(), e.getMessage(), e);
                throw new BizException(e.getMessage(), CommonErrorCode.ILLEGAL_ARGUMENT);
            }
        }


        try {
            Object result = pjp.proceed();
            log.info("[API] 请求结束 method={} cost={}ms result={}", method, stopWatch.getTime(), toJson(result));
            return result;
        } catch (Throwable t) {
            log.error("[API] 请求异常 method={} cost={}ms error={}", method, stopWatch.getTime(), t.getMessage(), t);
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