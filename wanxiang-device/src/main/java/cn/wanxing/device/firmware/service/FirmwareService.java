package cn.wanxing.device.firmware.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.firmware.dto.FirmwareUpgradeRequest;
import cn.wanxing.device.firmware.entity.FirmwareTask;
import cn.wanxing.device.firmware.mapper.FirmwareTaskMapper;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 固件升级服务：下发 ota_create，接收 ota_progress 进度（WebSocket 推送）与 services_reply 回执。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirmwareService {

    /** 固件升级进度 WebSocket 推送主题前缀（前端订阅 /topic/device/{sn}/firmware） */
    private static final String FIRMWARE_TOPIC_PREFIX = "/topic/device/";

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    private final FirmwareTaskMapper firmwareTaskMapper;

    private final DeviceMapper deviceMapper;

    private final UserContext userContext;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 下发固件升级任务：发布 ota_create 到 services 主题，并记录任务
     */
    public Boolean upgrade(String sn, FirmwareUpgradeRequest req) {
        // 1.校验设备存在 + 机构隔离
        User operator = userContext.currentUser();
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (device == null) {
            throw new DeviceException(DeviceErrorCode.DEVICE_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), device.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }

        // 2.组装 ota_create 消息
        ObjectNode message = objectMapper.createObjectNode();
        message.put("tid", UUID.randomUUID().toString());
        message.put("bid", UUID.randomUUID().toString());
        message.put("timestamp", System.currentTimeMillis());
        message.put("method", "ota_create");
        ObjectNode data = objectMapper.createObjectNode();
        ArrayNode devices = data.putArray("devices");
        ObjectNode d = devices.addObject();
        d.put("sn", sn);
        d.put("product_version", req.getTargetVersion());
        d.put("file_url", req.getFileUrl());
        d.put("md5", req.getMd5());
        d.put("file_size", req.getFileSize());
        d.put("file_name", req.getFileName());
        d.put("firmware_upgrade_type", req.getUpgradeType());
        message.set("data", data);

        // 3.发布到 services 主题
        String topic = "thing/product/" + sn + "/services";
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.FIRMWARE_UPGRADE_FAILED);
        }

        // 4.记录升级任务
        FirmwareTask task = FirmwareTask.create(sn, req);
        Assert.isTrue(firmwareTaskMapper.insert(task) > 0, () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));

        log.info("已下发固件升级 sn={} targetVersion={}", sn, req.getTargetVersion());
        return Boolean.TRUE;
    }

    /**
     * 处理固件升级进度（ota_progress，events 主题）：推送给前端，并更新任务状态
     */
    public void handleProgress(String sn, String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析 ota_progress 失败 sn={} payload={}", sn, payload, e);
            return;
        }
        JsonNode output = root.path("data").path("output");
        if (output == null || !output.isObject()) {
            return;
        }
        String status = output.path("status").asText(null);
        int percent = output.path("progress").path("percent").asInt(-1);
        log.info("固件升级进度 sn={} status={} percent={}", sn, status, percent);

        // 1.实时推送给订阅该设备的前端
        messagingTemplate.convertAndSend(FIRMWARE_TOPIC_PREFIX + sn + "/firmware", output);

        // 2.更新任务状态
        // todo:数据库是否扛得住
        updateTaskStatus(sn, status);
    }

    /**
     * 处理 services_reply 回执（ota_create 的下发结果）
     * todo:没有处理result
     */
    public void handleReply(String sn, String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析 services_reply 失败 sn={} payload={}", sn, payload, e);
            return;
        }
        if (!"ota_create".equals(root.path("method").asText(null))) {
            log.debug("非 ota_create 回执，忽略 sn={}", sn);
            return;
        }
        String status = root.path("data").path("output").path("status").asText(null);
        updateTaskStatus(sn, status);
        log.info("固件升级完成");
    }

    /**
     * 查询设备最近升级任务
     */
    public List<FirmwareTask> listTasks(String sn) {
        return firmwareTaskMapper.selectList(
                new LambdaQueryWrapper<FirmwareTask>().eq(FirmwareTask::getDeviceSn, sn)
                        .orderByDesc(FirmwareTask::getId).last("LIMIT 20"));
    }

    /**
     * 更新该设备最近一条升级任务的状态
     */
    private void updateTaskStatus(String sn, String status) {
        if (status == null) {
            return;
        }
        FirmwareTask task = firmwareTaskMapper.selectOne(
                new LambdaQueryWrapper<FirmwareTask>().eq(FirmwareTask::getDeviceSn, sn)
                        .orderByDesc(FirmwareTask::getId).last("LIMIT 1"));
        if (task == null) {
            return;
        }
        FirmwareTask.updateState(task, status);
        Assert.isTrue(firmwareTaskMapper.updateById(task) >  0, () -> new DeviceException(DeviceErrorCode.UPDATE_FAILED));
    }
}