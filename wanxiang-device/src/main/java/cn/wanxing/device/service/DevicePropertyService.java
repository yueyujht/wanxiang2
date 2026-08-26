package cn.wanxing.device.service;

import cn.wanxing.device.constant.DeviceTopicConst;
import cn.wanxing.device.dto.request.DevicePropertySetRequest;
import cn.wanxing.device.entity.Device;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.mapper.DeviceMapper;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * 设备属性服务：下发属性设置（property/set），并处理设备回执（property/set_reply）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DevicePropertyService {

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    private final DeviceMapper deviceMapper;

    private final UserContext userContext;

    /**
     * 设置设备属性：下发 property/set 命令到指定设备
     */
    public void setProperty(String sn, DevicePropertySetRequest req) {
        // 1.获取操作者，校验设备存在且在本机构范围内
        User operator = userContext.currentUser();
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (device == null) {
            throw new DeviceException(DeviceErrorCode.DEVICE_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), device.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }

        // 2.构造属性设置消息：data = {属性名: 属性值}
        ObjectNode message = objectMapper.createObjectNode();
        message.put("tid", UUID.randomUUID().toString());
        message.put("timestamp", System.currentTimeMillis());
        ObjectNode data = objectMapper.createObjectNode();
        data.set(req.getProperty(), req.getValue());
        message.set("data", data);

        // 3.发布到 property/set 主题
        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn + DeviceTopicConst.PROPERTY_SET_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.PROPERTY_SET_FAILED);
        }
        log.info("已下发设备属性设置 sn={} property={} value={}", sn, req.getProperty(), req.getValue());
    }

    /**
     * 处理属性设置回执：解析结果码（0 成功 / 1 失败 / 2 超时）
     */
    public void handleReply(String sn, String payload) {
        JsonNode reply;
        try {
            reply = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析属性设置回执失败 sn={} payload={}", sn, payload, e);
            return;
        }
        int result = reply.path("data").asInt(-1);
        log.info("设备属性设置结果 sn={} result={}（0 成功 / 1 失败 / 2 超时）", sn, result);
    }
}
