package cn.wanxing.device.mqtt;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * MQTT 消息日志切面：拦截标注了 {@link MqttLog} 的设备消息处理方法，记录消息体、耗时、异常。
 *
 * <p>设备消息处理方法的签名约定为 {@code (String sn/topic, String payload)}，
 * 首个参数是设备序列号（或主题），末个参数是消息原文（JSON 字符串）。
 * 日志级别可在 {@link MqttLog#level()} 上配置，高频消息用 DEBUG 避免刷屏。
 */
@Slf4j
@Aspect
@Component
public class MqttLogAspect {

    /** 日志单字段最大长度，超出截断，避免大消息刷屏 */
    private static final int MAX_LOG_LENGTH = 2000;

    @Around("@annotation(mqttLog)")
    public Object around(ProceedingJoinPoint pjp, MqttLog mqttLog) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();

        logAt(mqttLog.level(), "[MQTT] 收到消息 场景：{} handler={} args={}",
                mqttLog.value(), method.getName(), formatArgs(args));

        try {
            Object result = pjp.proceed();
            logAt(mqttLog.level(), "[MQTT] 处理完成 desc={} handler={} cost={}ms",
                    mqttLog.value(), method.getName(), stopWatch.getTime());
            return result;
        } catch (Throwable t) {
            log.error("[MQTT] 处理异常 desc={} handler={} cost={}ms error={}",
                    mqttLog.value(), method.getName(), stopWatch.getTime(), t.getMessage(), t);
            throw t;
        }
    }

    /**
     * 按注解配置的级别输出日志
     */
    private void logAt(Level level, String format, Object... args) {
        switch (level) {
            case TRACE -> log.trace(format, args);
            case DEBUG -> log.debug(format, args);
            case WARN -> log.warn(format, args);
            case ERROR -> log.error(format, args);
            default -> log.info(format, args);
        }
    }

    /**
     * 格式化入参：字符串（sn/topic、payload 原文）原样输出，避免 JSON 双重转义
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(toLog(args[i]));
        }
        return sb.toString();
    }

    private String toLog(Object obj) {
        if (obj == null) {
            return "null";
        }
        String text = obj instanceof String s ? s : JSON.toJSONString(obj);
        if (text.length() > MAX_LOG_LENGTH) {
            text = text.substring(0, MAX_LOG_LENGTH) + "...(截断)";
        }
        return text;
    }
}