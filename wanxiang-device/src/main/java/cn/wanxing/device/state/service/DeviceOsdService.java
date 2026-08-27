package cn.wanxing.device.state.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 设备 OSD 服务：处理 thing/product/{sn}/osd 高频遥测消息。
 *
 * <p>OSD 是 0.5Hz 定时上报的实时遥测（位置、航向、速度、电池、温度等），频率高、字段多。
 * 处理策略：不落库，把每台设备最新一条 OSD 缓存到 Redis，并实时推送给订阅了该设备的 WebSocket 客户端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DeviceOsdService {

    /** OSD 缓存 key 前缀 */
    private static final String OSD_KEY_PREFIX = "device:osd:";

    /** OSD 缓存过期时间：0.5Hz 上报会不断刷新，停止上报后 5 分钟自动过期 */
    private static final Duration OSD_TTL = Duration.ofMinutes(5);

    /** OSD WebSocket 推送主题前缀（前端订阅 /topic/device/{sn}/osd） */
    private static final String OSD_TOPIC_PREFIX = "/topic/device/";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 处理一条 OSD 消息：缓存到 Redis，并实时推送给订阅该设备的 WebSocket 客户端
     *
     * @param sn      主题中的设备序列号
     * @param payload 消息原文（JSON 字符串）
     */
    public void handleOsd(String sn, String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析设备 OSD 消息失败 sn={} payload={}", sn, payload, e);
            return;
        }
        JsonNode data = root.path("data");
        if (data == null || !data.isObject()) {
            return;
        }
        // 1.缓存最新一条（覆盖旧值并重置过期时间）
        stringRedisTemplate.opsForValue().set(OSD_KEY_PREFIX + sn, data.toString(), OSD_TTL);
        // 2.实时推送给订阅了该设备的 WebSocket 客户端
        messagingTemplate.convertAndSend(OSD_TOPIC_PREFIX + sn + "/osd", data);
        log.info("收到 OSD 并推送 WebSocket sn={} topic={}", sn, OSD_TOPIC_PREFIX + sn + "/osd");
    }

    /**
     * 查询设备最新一条 OSD 遥测，没有返回 null
     */
    public JsonNode getLatestOsd(String sn) {
        String json = stringRedisTemplate.opsForValue().get(OSD_KEY_PREFIX + sn);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.warn("解析缓存的 OSD 失败 sn={}", sn, e);
            return null;
        }
    }
}