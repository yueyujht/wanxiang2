package cn.wanxing.device.mqtt;

import org.slf4j.event.Level;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MQTT 设备消息日志注解：标注在设备消息处理方法上，自动记录消息来源、消息体、耗时、异常。
 *
 * <p>用法：{@code @MqttLog("设备上下线")}，value 为消息描述（可选）。
 * 高频消息（如 OSD 遥测、state 状态）建议指定 {@code level = Level.DEBUG} 避免刷屏。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttLog {

    /** 消息描述（可选，方便日志里识别） */
    String value() default "";

    /** 日志级别，默认 INFO */
    Level level() default Level.INFO;
}