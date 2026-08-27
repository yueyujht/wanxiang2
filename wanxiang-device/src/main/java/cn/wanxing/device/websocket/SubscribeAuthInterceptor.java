package cn.wanxing.device.websocket;

import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 订阅鉴权拦截器：校验订阅 /topic/device/{sn}/... 时，用户是否有权访问该设备（多租户机构隔离）。
 *
 * <p>规则：平台超管（无机构）可订阅任意设备；机构用户只能订阅本机构的设备。
 * 未登录、设备不存在或越权订阅时抛出异常，STOMP 会给客户端回 ERROR 帧并拒绝订阅。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscribeAuthInterceptor implements ChannelInterceptor {

    /** 设备实时数据订阅主题前缀 */
    private static final String DEVICE_TOPIC_PREFIX = "/topic/device/";

    private final UserMapper userMapper;

    private final DeviceMapper deviceMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        // 只校验设备订阅，其它主题放行
        String sn = parseSn(accessor.getDestination());
        if (sn == null) {
            return message;
        }

        // 取握手时存的登录用户 id
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Object loginId = attrs == null ? null : attrs.get("loginId");
        if (loginId == null) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        Long userId = Long.valueOf(loginId.toString());

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        // 平台超管（无机构）放行
        if (user.getOrgId() == null) {
            return message;
        }

        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (device == null || device.getOrgId() == null) {
            throw new DeviceException(DeviceErrorCode.DEVICE_NOT_FOUND);
        }
        // 机构用户只能订阅本机构设备
        if (!Objects.equals(user.getOrgId(), device.getOrgId())) {
            log.warn("用户 {} 越权订阅设备 {} 实时数据，拒绝", userId, sn);
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        return message;
    }

    /**
     * 从订阅主题中解析设备 SN：/topic/device/{sn}/... → sn，非设备主题返回 null
     */
    private String parseSn(String destination) {
        if (destination == null || !destination.startsWith(DEVICE_TOPIC_PREFIX)) {
            return null;
        }
        String rest = destination.substring(DEVICE_TOPIC_PREFIX.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}