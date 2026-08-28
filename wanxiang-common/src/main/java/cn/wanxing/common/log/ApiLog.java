package cn.wanxing.common.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口日志注解：标注在 Controller 方法上，自动记录请求入参、耗时、结果/异常。
 *
 * <p>用法：{@code @ApiLog("设备列表")}，value 为接口描述（可选）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    /** 接口描述（可选，方便日志里识别） */
    String value() default "";
}