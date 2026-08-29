package cn.wanxing.common.log;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 访问日志过滤器：所有请求统一记录一条访问日志，并维护 traceId 贯穿本次请求的所有日志。
 *
 * <p>覆盖全部接口（无需注解），弥补 Service 层 {@code @ApiLog} 覆盖不到的入口。
 * traceId 优先复用上游传来的 {@code X-Trace-Id} 头（网关/前端可传入），否则生成 UUID；
 * 同时回写到响应头，方便前端/支持人员把 traceId 反馈回来精确检索日志。
 */
@Slf4j
@Component
// -100：排在 RequestContextFilter(-105) 之后（Sa-Token 从请求上下文解析登录用户），其余业务过滤器之前
@Order(-100)
public class HttpRequestLogFilter extends OncePerRequestFilter {

    private static final String ANONYMOUS = "anonymous";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1.设置 traceId（复用上游或新生成），并回写响应头
        String incoming = request.getHeader(TraceContext.TRACE_ID_HEADER);
        String traceId = StringUtils.isNotBlank(incoming) ? incoming : UUID.randomUUID().toString();
        TraceContext.setTraceId(traceId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

        // 2.执行请求，finally 里统一记录访问日志并清理 MDC（防止线程复用串号）
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            String query = request.getQueryString();
            log.info("[HTTP] {} {}{} status={} cost={}ms user={} ip={}",
                    request.getMethod(), request.getRequestURI(),
                    query == null ? "" : "?" + query,
                    response.getStatus(), cost, currentUserId(), resolveIp(request));
            TraceContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        // ERROR dispatch（如 404 转发到 /error）不重复记录访问日志
        return true;
    }

    /**
     * 当前登录用户 id；过滤器阶段 Sa-Token 上下文可能未就绪（如最早的静态资源请求），此时记为 anonymous
     */
    private String currentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId != null ? String.valueOf(loginId) : ANONYMOUS;
        } catch (Exception e) {
            return ANONYMOUS;
        }
    }

    /**
     * 客户端 IP：优先 X-Forwarded-For 首段（经过 Nginx 等代理时），否则取直连地址
     */
    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
