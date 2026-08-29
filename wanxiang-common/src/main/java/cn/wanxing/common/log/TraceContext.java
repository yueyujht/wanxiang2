package cn.wanxing.common.log;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * traceId 上下文：基于 SLF4J MDC 的链路追踪。
 *
 * <p>一次请求（HTTP 请求 / 一条 MQTT 消息）分配一个 traceId，处理期间所有日志行自动携带
 * （logback pattern 中的 {@code %X{traceId}}），排查问题时按 traceId 一条 grep 捞出完整链路。
 *
 * <p>三个入口各自负责设置与清理：
 * <ul>
 *     <li>HTTP：{@link HttpRequestLogFilter} 进入时设置，finally 清理</li>
 *     <li>MQTT 入站：DeviceMessageHandler 用设备报文自带的 tid 设置（设备侧日志可直接互查）</li>
 *     <li>MQTT 出站：在调用线程上同步执行，自动继承，无需处理</li>
 * </ul>
 */
public final class TraceContext {

    /** MDC 中 traceId 的 key，logback pattern 通过 %X{traceId} 引用 */
    public static final String TRACE_ID_KEY = "traceId";

    /** HTTP 请求/响应头：透传 traceId，便于前端展示或网关串联 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceContext() {
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static String traceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 取当前 traceId，没有则生成一个 UUID。
     *
     * <p>用于 MQTT 下发指令的 tid：HTTP 触发的指令用请求的 traceId、MQTT 回执类响应用来向消息的
     * tid（设备回执原样带回），从而把「平台下发 → 设备执行 → 平台收到回执」串成同一个 id。
     * 使用标准 UUID 格式以兼容 DJI 协议的 tid 字段。
     */
    public static String traceIdOrNew() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return StringUtils.isNotBlank(traceId) ? traceId : UUID.randomUUID().toString();
    }
}
