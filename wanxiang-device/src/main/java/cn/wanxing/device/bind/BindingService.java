package cn.wanxing.device.bind;

import cn.wanxing.device.config.DjiProperties;
import cn.wanxing.device.config.OssProperties;
import cn.wanxing.device.device.constant.DeviceModelEnum;
import cn.wanxing.device.exception.BindErrorCode;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.mqtt.DeviceTopicConst;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 机场请求-应答服务：处理设备发来的 requests 消息，并回复 requests_reply。
 *
 * <p>处理五种 method：config（License 校验）、airport_bind_status（查询绑定状态）、
 * airport_organization_get（按绑定码查组织）、airport_organization_bind（执行绑定）、
 * storage_config_get（媒体上传临时凭证下发）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class BindingService {

    private static final int RESULT_SUCCESS = 0;

    /** DJI 通用错误码：参数不合法（无法识别的请求回复此码，避免设备一直等应答超时） */
    private static final int ERR_ILLEGAL_ARGUMENT = 200001;

    /** 设备域：机场（airport_organization_bind 中机场先建档，飞机挂到机场下） */
    private static final int DOMAIN_DOCK = 3;

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    private final DeviceMapper deviceMapper;

    private final OrgMapper orgMapper;

    private final DjiProperties djiProperties;

    private final OssProperties ossProperties;

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
        // 2.获取 method，根据 method 处理消息（method 缺失/未知时回错误应答，设备侧能立即感知而不是等超时）
        String method = request.getMethod();
        if (method == null) {
            log.warn("requests 消息缺少 method topic={} tid={}", topic, request.getTid());
            publishErrorReply(topic, request);
            return;
        }
        MethodResult result;
        switch (method) {
            case "config" -> {
                // 配置更新：当前仅支持 json 格式 + 产品维度（官方枚举），字段缺省放行兼容老固件，
                // 未知值回错误码让设备立即感知而不是等超时
                if (isSupportedConfig(request.getData())) {
                    publishConfigReply(topic, request);
                } else {
                    publishErrorReply(topic, request);
                }
                return;
            }
            case "storage_config_get" -> {
                publishStorageConfigReply(topic, request);
                return;
            }
            case "airport_bind_status" -> result = handleBindStatus(request.getData());
            case "airport_organization_get" -> result = handleOrganizationGet(request.getData());
            case "airport_organization_bind" -> result = handleOrganizationBind(request.getData());
            default -> {
                log.warn("未知的 requests method={} topic={}", method, topic);
                publishErrorReply(topic, request);
                return;
            }
        }
        publishReply(topic, request, method, result.result(), result.output());
    }

    // ============ 四个 method 的业务处理 ============

    /**
     * License 校验：返回应用凭据（app_id / app_key / app_license / ntp 服务器）
     *
     * <p>注意：config 的回执结构与组织绑定不同，字段直接平铺在 data 下，没有 result/output 包裹。
     * 凭据未配置时回空字符串（官方协议三个 app 字段必填，null 会导致设备端解析异常），并 ERROR 日志提示。
     */
    private void publishConfigReply(String topic, RequestsMessage request) {
        if (isBlank(djiProperties.getAppId()) || isBlank(djiProperties.getAppKey())
                || isBlank(djiProperties.getAppLicense())) {
            log.error("上云 License 未配置（wanxiang.dji.app-id/app-key/app-license），设备的 config 请求"
                    + "将拿到空凭据，上云流程会失败 topic={}", topic);
        }
        ObjectNode data = objectMapper.createObjectNode();
        data.put("ntp_server_host", orEmpty(djiProperties.getNtpServerHost()));
        data.put("app_id", orEmpty(djiProperties.getAppId()));
        data.put("app_key", orEmpty(djiProperties.getAppKey()));
        data.put("app_license", orEmpty(djiProperties.getAppLicense()));
        data.put("ntp_server_port", djiProperties.getNtpServerPort());
        publishReplyData(topic, request, "config", data);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 校验配置请求（官方"配置更新"页的枚举：config_type=json、config_scope=product），
     * 字段缺省按支持值放行，兼容未携带这些字段的老固件
     */
    private boolean isSupportedConfig(JsonNode data) {
        String type = data == null ? null : data.path("config_type").asText(null);
        String scope = data == null ? null : data.path("config_scope").asText(null);
        return (type == null || "json".equals(type)) && (scope == null || "product".equals(scope));
    }

    /**
     * 媒体上传临时凭证下发（storage_config_get，module=0 媒体）：
     * 字段与远程日志 fileupload_start 的凭证同源（wanxiang.oss.*），另带 object_key_prefix
     * 约定桶内目录——未配置时按网关 SN 隔离，设备把媒体文件直传对象存储后经 file_upload_callback 回报。
     */
    private void publishStorageConfigReply(String topic, RequestsMessage request) {
        if (isBlank(ossProperties.getBucket()) || isBlank(ossProperties.getAccessKeyId())) {
            log.error("对象存储未配置（wanxiang.oss.bucket/access-key-id 等），设备的 storage_config_get 将拿到空凭证，"
                    + "媒体上传会失败 topic={}", topic);
        }
        // storage_config_get 的回执结构与 config 一样平铺在 output 下（外层 result 由 publishReply 统一包 0）
        ObjectNode output = objectMapper.createObjectNode();
        output.put("bucket", ossProperties.getBucket());
        output.put("region", ossProperties.getRegion());
        output.put("endpoint", ossProperties.getEndpoint());
        output.put("provider", ossProperties.getProvider());
        ObjectNode credentials = output.putObject("credentials");
        credentials.put("access_key_id", ossProperties.getAccessKeyId());
        credentials.put("access_key_secret", ossProperties.getAccessKeySecret());
        credentials.put("expire", System.currentTimeMillis() + 3600000L);
        credentials.put("security_token", ossProperties.getSecurityToken());
        output.put("object_key_prefix", isBlank(ossProperties.getObjectKeyPrefix())
                ? defaultObjectKeyPrefix(topic) : ossProperties.getObjectKeyPrefix());
        publishReply(topic, request, "storage_config_get", RESULT_SUCCESS, output);
    }

    /**
     * 缺省的存储目录前缀：取主题中的网关 SN（thing/product/{sn}/requests），实现桶内按机场隔离
     */
    private String defaultObjectKeyPrefix(String topic) {
        int start = topic.indexOf(DeviceTopicConst.PRODUCT);
        if (start < 0) {
            return "";
        }
        start += DeviceTopicConst.PRODUCT.length();
        int end = topic.indexOf("/", start);
        return end < 0 ? topic.substring(start) : topic.substring(start, end);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 查询设备绑定状态：返回每台设备是否已绑定组织
     */
    private MethodResult handleBindStatus(JsonNode data) {
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
        return MethodResult.ok(output);
    }

    // 构建绑定状态
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
            return new MethodResult(BindErrorCode.GET_ORGANIZATION_FAILED, output);
        }
        output.put("organization_name", org.getName());
        return MethodResult.ok(output);
    }

    /**
     * 将设备 SN 绑定到绑定码对应的机构
     *
     * <p>绑定流程对照官方 demo：机场（domain=3）先建档，同批飞机等子设备的 parent_sn 指向同批机场，
     * 保证绑定后父子拓扑完整（否则设备树断裂，依赖 parent_sn 的网关解析也会失效）。
     */
    private MethodResult handleOrganizationBind(JsonNode data) {
        ObjectNode output = objectMapper.createObjectNode();
        ArrayNode errInfos = output.putArray("err_infos");
        JsonNode bindDevices = data == null ? null : data.get("bind_devices");
        if (bindDevices == null || !bindDevices.isArray()) {
            return MethodResult.ok(output);
        }

        // 第一遍绑机场并记住 SN，第二遍绑其余设备（飞机/负载），err_infos 仍按请求顺序输出
        Map<String, Integer> results = new LinkedHashMap<>();
        String dockSn = null;
        for (JsonNode device : bindDevices) {
            if (isDock(device)) {
                String sn = device.path("sn").asText();
                results.put(sn, bindDevice(sn, device, null));
                if (results.get(sn) == RESULT_SUCCESS && dockSn == null) {
                    dockSn = sn;
                }
            }
        }
        for (JsonNode device : bindDevices) {
            if (!isDock(device)) {
                String sn = device.path("sn").asText();
                results.put(sn, bindDevice(sn, device, dockSn));
            }
        }
        for (JsonNode device : bindDevices) {
            String sn = device.path("sn").asText();
            ObjectNode info = errInfos.addObject();
            info.put("sn", sn);
            info.put("err_code", results.getOrDefault(sn, BindErrorCode.DEVICE_BINDING_FAILED));
        }
        return MethodResult.ok(output);
    }

    /** 按 device_model_key 的 domain 判断是否机场 */
    private boolean isDock(JsonNode device) {
        return applyModelKey(device.path("device_model_key").asText())[0] == DOMAIN_DOCK;
    }

    /**
     * 将设备根据绑定码绑定到机构
     *
     * @param parentSn 同批绑定的机场 SN（非机场设备挂到其下；无同批机场时为 null，等拓扑上报补全）
     */
    private int bindDevice(String sn, JsonNode device, String parentSn) {
        String bindCode = device.path("device_binding_code").asText();
        String organizationId = device.path("organization_id").asText();
        String callsign = device.path("device_callsign").asText();
        String modelKey = device.path("device_model_key").asText();

        // 1.查询机构：优先按绑定码查，查不到回退到 organization_id（官方 demo 的兜底逻辑）
        Org org = (bindCode == null || bindCode.isEmpty()) ? null
                : orgMapper.selectOne(new LambdaQueryWrapper<Org>().eq(Org::getBindCode, bindCode));
        if (org == null && organizationId != null && !organizationId.isEmpty()) {
            try {
                org = orgMapper.selectById(Long.valueOf(organizationId));
            } catch (NumberFormatException ignored) {
                // 非法的 organization_id，忽略回退
            }
        }
        if (org == null) {
            return BindErrorCode.DEVICE_BINDING_FAILED;
        }

        // 2.根据设备sn查询设备
        // 这里有三种情况：a.没有设备，新建；b.有设备，是老设备解除绑定；c.有设备，但已经绑定到其他组织
        // 情况c，返回错误码
        Device deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (deviceEntity != null && deviceEntity.getOrgId() != null && !deviceEntity.getOrgId().equals(org.getId())) {
            return BindErrorCode.NON_REPEATABLE_BINDING;
        }
        if (deviceEntity == null) {
            // 情况a：新建建档，携带父子关系与绑定时间
            int[] modelKeys = applyModelKey(modelKey);
            String modelName = DeviceModelEnum.resolveName(modelKeys[0], modelKeys[1], modelKeys[2]);
            deviceEntity = Device.create(sn, callsign, org.getId(), modelKeys, modelName,
                    parentSn, null, null, null, false);
            deviceEntity.setBoundAt(LocalDateTime.now());
            deviceMapper.insert(deviceEntity);
        } else {
            // 情况b
            Device.updateForRebind(deviceEntity, org.getId(), callsign);
            deviceMapper.updateById(deviceEntity);
        }
        return 0;
    }

    /**
     * 解析 device_model_key（如 3-1-0）
     */
    private int[] applyModelKey(String modelKey) {
        if (modelKey == null || modelKey.isEmpty()) {
            return new int[]{-1,-1,-1};
        }
        String[] parts = modelKey.split("-");
        if (parts.length != 3) {
            return new int[]{-1,-1,-1};
        }
        int[] modelKeys;
        try{
            modelKeys = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
        }catch (NumberFormatException e){
            return new int[]{-1,-1,-1};
        }
        return modelKeys;
    }

    // ============ 回复 ============

    /**
     * 构造回复信封并发布到 requests_reply 主题，回传相同的 tid/bid/method
     *
     * @param result 返回码：0 成功，非 0 错误（DJI 标准错误码）
     */
    private void publishReply(String topic, RequestsMessage request, String method, int result, ObjectNode output) {
        // 组织绑定类方法：data 部分 result 非 0 代表错误，output 放方法对应的结果
        ObjectNode data = objectMapper.createObjectNode();
        data.put("result", result);
        data.set("output", output);
        publishReplyData(topic, request, method, data);
    }

    /**
     * 组装回复信封并发布到 requests_reply 主题（请求主题 + "_reply"）
     */
    private void publishReplyData(String topic, RequestsMessage request, String method, JsonNode data) {
        // 回传相同的 tid / bid / method（设备靠它们匹配请求与应答），gateway 字段对照官方示例回传
        ObjectNode reply = objectMapper.createObjectNode();
        reply.put("tid", request.getTid());
        reply.put("bid", request.getBid());
        if (request.getGateway() != null) {
            reply.put("gateway", request.getGateway());
        }
        reply.put("timestamp", System.currentTimeMillis());
        if (method != null) {
            reply.put("method", method);
        }
        reply.set("data", data);
        try {
            mqttPublisher.publish(topic + "_reply", objectMapper.writeValueAsString(reply), "回复设备请求 " + method);
        } catch (JsonProcessingException e) {
            log.error("序列化回复消息失败 method={}", method, e);
        }
    }

    /**
     * 回复一条通用错误（如 method 缺失/未知），避免设备等应答直到超时
     */
    private void publishErrorReply(String topic, RequestsMessage request) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("result", ERR_ILLEGAL_ARGUMENT);
        publishReplyData(topic, request, request.getMethod(), data);
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
