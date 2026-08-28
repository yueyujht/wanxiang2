package cn.wanxing.device.status.service;

import cn.wanxing.common.log.ApiLog;
import cn.wanxing.device.mqtt.DeviceTopicConst;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.mqtt.MqttLog;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.device.status.constant.DevicePropertyEnum;
import cn.wanxing.device.status.dto.DevicePropertySchemaVO;
import cn.wanxing.device.status.dto.DevicePropertySetRequest;
import cn.wanxing.device.status.entity.DeviceState;
import cn.wanxing.device.status.mapper.DeviceStateMapper;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 设备属性服务：下发属性设置（property/set），处理设备回执（property/set_reply），并提供可设置属性字典。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DevicePropertyService {

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    private final DeviceMapper deviceMapper;

    private final DeviceStateMapper deviceStateMapper;

    private final UserContext userContext;

    /**
     * 设置设备属性：下发 property/set 命令到指定设备
     * sys_device_state的更新在设备发送state消息的时候
     */
    @ApiLog("设置设备属性")
    public Boolean setProperty(String sn, DevicePropertySetRequest req) {
        // 1.校验设备存在且在本机构范围内
        checkAccess(sn);

        // 2.校验属性值：在字典中的属性做类型/值域校验；未知属性（其它型号尚未收录）透传不拦截
        DevicePropertyEnum dict = DevicePropertyEnum.of(req.getProperty());
        if (dict != null && !dict.isValid(req.getValue())) {
            throw new DeviceException(DeviceErrorCode.PROPERTY_VALUE_INVALID);
        }

        // 3.构造属性设置消息：data = {属性名: 属性值}，带 bid/tid 供回执关联
        ObjectNode message = objectMapper.createObjectNode();
        message.put("tid", UUID.randomUUID().toString());
        message.put("bid", UUID.randomUUID().toString());
        message.put("timestamp", System.currentTimeMillis());
        ObjectNode data = objectMapper.createObjectNode();
        data.set(req.getProperty(), req.getValue());
        message.set("data", data);

        // 4.发布到 property/set 主题
        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn + DeviceTopicConst.PROPERTY_SET_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.PROPERTY_SET_FAILED);
        }
        log.info("已下发设备属性设置命令 sn={} property={} value={} mqtt消息: ", sn, req.getProperty(), req.getValue());
        log.info(message.toString());
        return Boolean.TRUE;
    }

    /**
     * 处理属性设置回执：解析每个属性的结果码（result：0 成功 / 1 失败 / 2 超时）
     */
    @MqttLog("属性设置回执")
    public void handleReply(String sn, String payload) {
        // 1.读取Json消息内容
        JsonNode reply;
        try {
            reply = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析属性设置回执失败 sn={} payload={}", sn, payload, e);
            return;
        }

        // 回执结构：data = {属性名: {state: {result}}}，遍历每个属性记录结果
        JsonNode data = reply.path("data");
        if (data == null || !data.isObject()) {
            log.warn("属性设置回执缺少 data 字段 sn={} payload={}", sn, payload);
            return;
        }
        data.fields().forEachRemaining(entry -> {
            JsonNode state = entry.getValue().path("state");
            int result = state.path("result").asInt(-1);
            if(result == 0){
                log.info("设备属性设置成功 sn={} property={} result={}（result: 0 成功 / 1 失败 / 2 超时）",
                        sn, entry.getKey(), result);
            } else {
                log.warn("设备属性设置失败 sn={} property={} result={}（result: 0 成功 / 1 失败 / 2 超时）",
                        sn, entry.getKey(), result);
            }

        });
    }

    /**
     * 返回可设置属性字典 + 各属性当前值（供前端渲染设置界面）
     */
    @ApiLog("属性 schema")
    public List<DevicePropertySchemaVO> listSchema(String sn) {
        checkAccess(sn);
        DeviceState state = deviceStateMapper.selectOne(
                new LambdaQueryWrapper<DeviceState>().eq(DeviceState::getDeviceSn, sn));
        return Arrays.stream(DevicePropertyEnum.values())
                .map(p -> DevicePropertySchemaVO.from(p, currentValue(p, state)))
                .toList();
    }

    /**
     * 从最新 state 里取某个属性的当前值，设备未上报返回 null
     */
    private Object currentValue(DevicePropertyEnum p, DeviceState state) {
        if (state == null) {
            return null;
        }
        return switch (p) {
            case AIR_TRANSFER_ENABLE -> state.getAirTransferEnable();
            case SILENT_MODE -> state.getSilentMode();
            case USER_EXPERIENCE_IMPROVEMENT -> state.getUserExperienceImprovement();
        };
    }

    /**
     * 校验设备存在 + 机构隔离
     */
    private void checkAccess(String sn) {
        User operator = userContext.currentUser();
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (device == null) {
            throw new DeviceException(DeviceErrorCode.DEVICE_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), device.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
    }
}