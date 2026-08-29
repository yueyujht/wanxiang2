package cn.wanxing.device.status.service;

import cn.wanxing.common.log.ApiLog;
import cn.wanxing.device.device.constant.DeviceStatusEnum;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 *
 * <p>OSD 同时是在线心跳：每条消息刷新心跳 key（TTL 即离线阈值），
 * 设备重新开始上报且库里仍是离线时置在线；心跳超时的离线判定在 {@link DeviceOfflineChecker}。
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

    /** 心跳 key 前缀（设备离线检测的在线依据，与 OSD 缓存分离，互不影响过期时间） */
    public static final String HEARTBEAT_KEY_PREFIX = "device:heartbeat:";

    /** 心跳超时：OSD 约 2s 一条，90 秒未上报视为离线（官方 demo 阈值 60 秒，此处稍宽松防误判） */
    public static final Duration HEARTBEAT_TTL = Duration.ofSeconds(90);

    /** WebSocket 设备主题前缀 */
    static final String DEVICE_TOPIC_PREFIX = "/topic/device/";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    private final DeviceMapper deviceMapper;

    /**
     * 处理一条 OSD 消息：缓存到 Redis、刷新心跳，并实时推送给订阅该设备的 WebSocket 客户端
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
        // 1.缓存最新一条（覆盖旧值并重置过期时间）+ 刷新心跳
        //   先读心跳 key 是否存在：不存在说明是（重新）开始上报，需要做一次状态跃迁检查
        boolean hadHeartbeat = Boolean.TRUE.equals(stringRedisTemplate.hasKey(HEARTBEAT_KEY_PREFIX + sn));
        stringRedisTemplate.opsForValue().set(OSD_KEY_PREFIX + sn, data.toString(), OSD_TTL);
        stringRedisTemplate.opsForValue().set(HEARTBEAT_KEY_PREFIX + sn, "1", HEARTBEAT_TTL);
        // 2.心跳恢复：库里仍是离线则置在线（仅跃迁写库，稳态上报不产生额外查库）
        if (!hadHeartbeat) {
            markOnlineIfOffline(sn);
        }
        // 3.实时推送给订阅了该设备的 WebSocket 客户端（推送失败不影响缓存与后续消息）
        try {
            messagingTemplate.convertAndSend(DEVICE_TOPIC_PREFIX + sn + "/osd", objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            log.warn("OSD 推送序列化失败 sn={}", sn, e);
        }
    }

    /**
     * 查询设备最新一条 OSD 遥测，没有返回 null
     */
    @ApiLog("设备 OSD 遥测")
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

    /**
     * 心跳恢复：设备重新开始上报 OSD，库里仍是离线则置在线并推送 WS
     */
    private void markOnlineIfOffline(String sn) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (device == null || device.getStatus() == DeviceStatusEnum.ONLINE) {
            return;
        }
        Device.updateForTopo(device, true);
        deviceMapper.updateById(device);
        log.info("设备心跳恢复，置为在线 sn={}", sn);
        pushStatus(sn, true);
    }

    /**
     * 在线状态变化推送：{sn, online}
     */
    private void pushStatus(String sn, boolean online) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sn", sn);
        payload.put("online", online);
        try {
            messagingTemplate.convertAndSend(DEVICE_TOPIC_PREFIX + sn + "/status",
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("设备状态推送序列化失败 sn={}", sn, e);
        }
    }
}
