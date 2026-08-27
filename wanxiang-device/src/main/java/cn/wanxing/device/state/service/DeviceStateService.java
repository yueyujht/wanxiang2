package cn.wanxing.device.state.service;

import cn.wanxing.device.state.mapper.DeviceStateMapper;
import cn.wanxing.device.state.entity.DeviceState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备状态服务：处理 thing/product/{sn}/state 消息。
 *
 * <p>state 消息是「增量上报」——设备只在状态变化时上报，消息里只含变化的字段。
 * 落库时通过 SQL 的 COALESCE 保证：本次未上报的字段保持历史值不变（不会被覆盖成 null）。
 * 字段解析在 {@link DeviceState#update}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStateService {

    private final DeviceStateMapper deviceStateMapper;

    private final ObjectMapper objectMapper;

    /**
     * 处理一条 state 消息
     *
     * @param sn      主题中的主设备序列号
     * @param payload 消息原文（JSON 字符串）
     */
    public void handleState(String sn, String payload) {
        // 1.解析消息原文，取出 data（增量：只含变化的字段）
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析设备 state 消息失败 sn={} payload={}", sn, payload, e);
            return;
        }
        JsonNode data = root.path("data");
        if (data == null || !data.isObject()) {
            return;
        }

        // 2.把最新状态 UPSERT 到 sys_device_state（增量字段由 SQL 的 COALESCE 保证不覆盖历史值）
        DeviceState deviceState = DeviceState.update(sn, data);
        deviceStateMapper.upsert(deviceState);
    }
}