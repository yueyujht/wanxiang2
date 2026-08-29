package cn.wanxing.device.status.service;

import cn.wanxing.device.device.constant.DeviceStatusEnum;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备离线检测定时任务：设备断电/断网后不会再发 update_topo，
 * 在线判定以 OSD 心跳为准（{@link DeviceOsdService} 每条 OSD 刷新 Redis 心跳 key），超时未续期即置离线。
 *
 * <p>每分钟扫描「库里在线但心跳 key 已过期」的设备置 OFFLINE，并推 WS（/topic/device/{sn}/status）供前端刷新列表。
 * 仅配置了 MQTT 时生效（与 OSD 处理同条件）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DeviceOfflineChecker {

    /** 扫描间隔：每分钟一次，叠加 90 秒心跳超时，最长约 2.5 分钟检出离线 */
    private static final long SWEEP_INTERVAL_MS = 60_000;

    private final DeviceMapper deviceMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 扫描在线设备：心跳 key 已过期（超时无 OSD）的置离线
     */
    @Scheduled(fixedDelay = SWEEP_INTERVAL_MS, initialDelay = SWEEP_INTERVAL_MS)
    public void sweep() {
        List<Device> onlineDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().eq(Device::getStatus, DeviceStatusEnum.ONLINE));
        for (Device device : onlineDevices) {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(DeviceOsdService.HEARTBEAT_KEY_PREFIX + device.getSn()))) {
                continue;
            }
            Device.updateForTopo(device, false);
            deviceMapper.updateById(device);
            log.info("设备心跳超时，置为离线 sn={}", device.getSn());
            pushStatus(device.getSn(), false);
        }
    }

    /**
     * 在线状态变化推送：{sn, online}
     */
    private void pushStatus(String sn, boolean online) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sn", sn);
        payload.put("online", online);
        try {
            messagingTemplate.convertAndSend(DeviceOsdService.DEVICE_TOPIC_PREFIX + sn + "/status",
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("设备状态推送序列化失败 sn={}", sn, e);
        }
    }
}
