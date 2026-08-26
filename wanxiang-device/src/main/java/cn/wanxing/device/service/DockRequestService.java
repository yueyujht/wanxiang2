package cn.wanxing.device.service;

import cn.wanxing.device.config.DjiProperties;
import cn.wanxing.device.constant.DeviceModelEnum;
import cn.wanxing.device.constant.DeviceStatusEnum;
import cn.wanxing.device.constant.DockErrorCode;
import cn.wanxing.device.dto.RequestsMessage;
import cn.wanxing.device.entity.Device;
import cn.wanxing.device.mapper.DeviceMapper;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.user.entity.Org;
import cn.wanxing.user.mapper.OrgMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 机场请求-应答服务：处理设备发来的 requests 消息，并回复 requests_reply。
 *
 * <p>处理四种 method：config（License 校验）、airport_bind_status（查询绑定状态）、
 * airport_organization_get（按绑定码查组织）、airport_organization_bind（执行绑定）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class DockRequestService {

    private static final int RESULT_SUCCESS = 0;

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    private final DeviceMapper deviceMapper;

    private final OrgMapper orgMapper;

    private final DjiProperties djiProperties;

    /**
     * 入口：解析请求信封，按 method 分发，最后回复 requests_reply
     */
    public void handleRequest(String topic, String payload) {
        // 1.将请求体转换成消息对象
        RequestsMessage request;
        try {
            request = objectMapper.readValue(payload, RequestsMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("解析设备请求消息失败 topic={} payload={}", topic, payload, e);
            return;
        }
        // 2.获取 method，根据 method 处理消息
        String method = request.getMethod();
        MethodResult result;
        switch (method) {
            case "config" -> result = MethodResult.ok(handleConfig());
            case "airport_bind_status" -> result = MethodResult.ok(handleBindStatus(request.getData()));
            case "airport_organization_get" -> result = handleOrganizationGet(request.getData());
            case "airport_organization_bind" -> result = MethodResult.ok(handleOrganizationBind(request.getData()));
            default -> {
                log.warn("未知的 requests method={}", method);
                return;
            }
        }
        publishReply(topic, request, method, result.result(), result.output());
    }

    // ============ 四个 method 的业务处理 ============

    /**
     * License 校验：返回应用凭据（app_id / app_key / app_license / ntp 服务器）
     */
    private ObjectNode handleConfig() {
        // 组装 License 校验响应：把应用凭据下发给设备
        ObjectNode output = objectMapper.createObjectNode();
        output.put("ntp_server_host", djiProperties.getNtpServerHost());
        output.put("app_id", djiProperties.getAppId());
        output.put("app_key", djiProperties.getAppKey());
        output.put("app_license", djiProperties.getAppLicense());
        return output;
    }

    /**
     * 查询设备绑定状态：返回每台设备是否已绑定组织
     */
    private ObjectNode handleBindStatus(JsonNode data) {
        // 遍历请求里的每台设备，逐个查询绑定状态
        ArrayNode bindStatus = objectMapper.createArrayNode();
        JsonNode devices = data == null ? null : data.get("devices");
        if (devices != null && devices.isArray()) {
            for (JsonNode device : devices) {
                bindStatus.add(buildBindStatusItem(device.path("sn").asText()));
            }
        }
        ObjectNode output = objectMapper.createObjectNode();
        output.set("bind_status", bindStatus);
        return output;
    }

    private ObjectNode buildBindStatusItem(String sn) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("sn", sn);

        // 1.查设备是否已绑定（有 org_id 即视为已绑定）
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        boolean bound = device != null && device.getOrgId() != null;
        item.put("is_device_bind_organization", bound);

        // 2.已绑定则回传机构信息，未绑定则回传空
        if (bound) {
            Org org = orgMapper.selectById(device.getOrgId());
            item.put("organization_id", org == null ? "" : String.valueOf(org.getId()));
            item.put("organization_name", org == null ? "" : org.getName());
        } else {
            item.put("organization_id", "");
            item.put("organization_name", "");
        }
        // 3.设备名称（绑定时填写的 device_callsign）
        item.put("device_callsign", device == null || device.getName() == null ? "" : device.getName());
        return item;
    }

    /**
     * 查询绑定码对应的组织名
     */
    private MethodResult handleOrganizationGet(JsonNode data) {
        // 1.取出设备填写的绑定码
        String bindCode = data == null ? "" : data.path("device_binding_code").asText();
        // 2.按绑定码查机构
        Org org = (bindCode == null || bindCode.isEmpty()) ? null
                : orgMapper.selectOne(new LambdaQueryWrapper<Org>().eq(Org::getBindCode, bindCode));

        ObjectNode output = objectMapper.createObjectNode();
        if (org == null) {
            // 3.机构找不到，返回错误码 210230
            output.put("organization_name", "");
            return new MethodResult(DockErrorCode.GET_ORGANIZATION_FAILED, output);
        }
        output.put("organization_name", org.getName());
        return MethodResult.ok(output);
    }

    /**
     * 执行绑定：把设备 SN 绑定到绑定码对应的机构
     */
    private ObjectNode handleOrganizationBind(JsonNode data) {
        // 遍历要绑定的设备，逐个执行绑定并记录结果
        ArrayNode errInfos = objectMapper.createArrayNode();
        JsonNode bindDevices = data == null ? null : data.get("bind_devices");
        if (bindDevices != null && bindDevices.isArray()) {
            for (JsonNode device : bindDevices) {
                String sn = device.path("sn").asText();
                String bindCode = device.path("device_binding_code").asText();
                String callsign = device.path("device_callsign").asText();
                String modelKey = device.path("device_model_key").asText();
                int errCode = bindDevice(sn, bindCode, callsign, modelKey);
                ObjectNode info = objectMapper.createObjectNode();
                info.put("sn", sn);
                info.put("err_code", errCode);
                errInfos.add(info);
            }
        }
        ObjectNode output = objectMapper.createObjectNode();
        output.set("err_infos", errInfos);
        return output;
    }

    /**
     * 把设备 SN 绑定到绑定码对应的机构；返回 0 成功，210231 绑定失败
     */
    private int bindDevice(String sn, String bindCode, String callsign, String modelKey) {
        // 1.按绑定码查机构，绑定码无效返回 210231
        Org org = orgMapper.selectOne(new LambdaQueryWrapper<Org>().eq(Org::getBindCode, bindCode));
        if (org == null) {
            return DockErrorCode.DEVICE_BINDING_FAILED;
        }
        // 2.查设备是否存在，不存在则新建（初始为离线）
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        boolean isNew = device == null;
        if (isNew) {
            device = new Device();
            device.setSn(sn);
            device.setStatus(DeviceStatusEnum.OFFLINE);
        } else if (device.getOrgId() != null && !device.getOrgId().equals(org.getId())) {
            // 已绑定到其它机构，不可重复绑定
            return DockErrorCode.NON_REPEATABLE_BINDING;
        }
        // 3.绑定：设置所属机构 + 设备名称 + 设备型号（device_model_key），并落库
        device.setOrgId(org.getId());
        if (callsign != null && !callsign.isEmpty()) {
            device.setName(callsign);
        }
        applyModelKey(device, modelKey);
        if (isNew) {
            deviceMapper.insert(device);
        } else {
            deviceMapper.updateById(device);
        }
        return 0;
    }

    /**
     * 解析 device_model_key（如 3-1-0），把 domain / type / sub_type / 型号名称存到设备
     */
    private void applyModelKey(Device device, String modelKey) {
        if (modelKey == null || modelKey.isEmpty()) {
            return;
        }
        String[] parts = modelKey.split("-");
        if (parts.length < 3) {
            return;
        }
        Integer domain = parseIntSafe(parts[0]);
        Integer type = parseIntSafe(parts[1]);
        Integer subType = parseIntSafe(parts[2]);
        device.setDomain(domain);
        device.setType(type);
        device.setSubType(subType);
        device.setModelName(DeviceModelEnum.resolveName(domain, type, subType));
    }

    private static Integer parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ============ 回复 ============

    /**
     * 构造回复信封并发布到 requests_reply 主题，回传相同的 tid/bid/method
     *
     * @param result 返回码：0 成功，非 0 错误（DJI 标准错误码）
     */
    private void publishReply(String topic, RequestsMessage request, String method, int result, ObjectNode output) {
        // 1.组装回复信封：回传相同的 tid / bid / method（设备靠它们匹配请求与应答）
        ObjectNode reply = objectMapper.createObjectNode();
        reply.put("tid", request.getTid());
        reply.put("bid", request.getBid());
        reply.put("timestamp", System.currentTimeMillis());
        reply.put("method", method);

        // 2.数据部分：result 非 0 代表错误，output 放方法对应的结果
        ObjectNode data = objectMapper.createObjectNode();
        data.put("result", result);
        data.set("output", output);
        reply.set("data", data);

        // 3.发布到 requests_reply 主题（请求主题 + "_reply"）
        try {
            mqttPublisher.publish(topic + "_reply", objectMapper.writeValueAsString(reply));
        } catch (JsonProcessingException e) {
            log.error("序列化回复消息失败 method={}", method, e);
        }
    }

    /**
     * 方法处理结果：返回码 + 输出体
     */
    private record MethodResult(int result, ObjectNode output) {
        static MethodResult ok(ObjectNode output) {
            return new MethodResult(RESULT_SUCCESS, output);
        }
    }
}
