package cn.wanxing.device.websocket;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手鉴权：从 query 参数取 sa-token 校验登录态。
 *
 * <p>浏览器原生 WebSocket 无法自定义请求头，token 通过 query 参数传递：
 * {@code ws://host/ws?satoken={token}}。未登录或 token 失效则拒绝握手。
 */
@Slf4j
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("satoken");
        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket 握手缺少 token，拒绝连接");
            return false;
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            log.warn("WebSocket 握手 token 无效，拒绝连接");
            return false;
        }
        attributes.put("loginId", loginId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}