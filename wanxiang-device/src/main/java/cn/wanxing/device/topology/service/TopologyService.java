package cn.wanxing.device.topology.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.device.constant.DeviceModelEnum;
import cn.wanxing.device.device.constant.DeviceStatusEnum;
import cn.wanxing.device.mqtt.DeviceTopicConst;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.device.topology.entity.SubDeviceInfo;
import cn.wanxing.device.topology.entity.TopologyData;
import cn.wanxing.device.topology.entity.TopologyMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 拓扑/上下线服务：处理 sys/product/{sn}/status 消息（update_topo），维护设备拓扑并回 status_reply。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopologyService {

    private final DeviceMapper deviceMapper;

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    /**
     * 处理一条设备上下线消息：仅处理已绑定设备，未绑定直接忽略
     *
     * @param sn      从主题解析出的设备序列号（主设备，如机场）
     * @param payload 消息原文（JSON 字符串）
     */
    public void handleStatus(String sn, String payload) {
        // 1.解析上下线消息为对象
        TopologyMessage message;
        try {
            message = objectMapper.readValue(payload, TopologyMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("解析设备上下线消息失败 sn={} payload={}", sn, payload, e);
            return;
        }
        TopologyData data = message.getData();
        if (data == null) {
            return;
        }

        // 2.主设备必须是已绑定设备，否则忽略（未绑定设备不追踪）
        Device mainDevice = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (mainDevice == null) {
            log.warn("收到未绑定设备的上下线消息，已忽略 sn={}", sn);
            return;
        }

        // 3.更新主设备状态（在线时记录最近上线时间）
        boolean online = data.getSubDevices() != null && !data.getSubDevices().isEmpty();
        Device.updateForTopo(mainDevice, data.getDeviceSecret(), data.getNonce(), data.getThingVersion(), online);
        Assert.isTrue(deviceMapper.updateById(mainDevice) > 0, () -> new DeviceException(DeviceErrorCode.UPDATE_FAILED));

        // 4.子设备对账：以 sub_devices 列表为准（列表里 upsert 在线，不在列表里的置离线）
        List<SubDeviceInfo> subDevices = data.getSubDevices() == null ? Collections.emptyList() : data.getSubDevices();
        // 4.1 遍历消息里的子设备，upsert 为在线（不存在则新建）
        for (SubDeviceInfo sub : subDevices) {
            upsertChild(mainDevice, sub);
        }
        // 4.2 遍历 DB 里已有的子设备，不在消息列表里的标记离线
        Set<String> onlineSns = subDevices.stream().map(SubDeviceInfo::getSn).collect(Collectors.toSet());
        List<Device> childDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().eq(Device::getParentSn, sn));
        for (Device childDevice : childDevices) {
            if (!onlineSns.contains(childDevice.getSn())) {
                Device.updateForTopo(childDevice, false);
                childDevice.setStatus(DeviceStatusEnum.OFFLINE);
                deviceMapper.updateById(childDevice);
            }
        }

        // 5.回 status_reply 确认（协议要求云端收到拓扑更新后回执 result=0）
        sendStatusReply(sn, message);
    }

    /**
     * 回 status_reply 确认：status 是拓扑/上下线消息（区别于 state 状态消息），回 result=0 表示已收到
     */
    private void sendStatusReply(String sn, TopologyMessage message) {
        ObjectNode reply = objectMapper.createObjectNode();
        reply.put("tid", message.getTid());
        reply.put("bid", message.getBid());
        reply.put("timestamp", System.currentTimeMillis());
        reply.put("method", message.getMethod() == null ? "update_topo" : message.getMethod());
        ObjectNode data = objectMapper.createObjectNode();
        data.put("result", 0);
        reply.set("data", data);

        String topic = DeviceTopicConst.SYS_PRE + DeviceTopicConst.PRODUCT + sn
                + DeviceTopicConst.STATUS_SUF + DeviceTopicConst.REPLY_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(reply));
        } catch (JsonProcessingException e) {
            log.error("序列化 status_reply 失败 sn={}", sn, e);
        }
    }

    /**
     * 处理子设备上线（如机场内的无人机）：按 SN 查询，不存在则新建，并继承父设备的机构
     */
    private void upsertChild(Device mainDevice, SubDeviceInfo sub) {
        // 1.按 SN 查子设备，不存在则新建
        Device childDevice = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sub.getSn()));
        boolean isNew = childDevice == null;
        if (isNew) {
            // 新建子设备
            int[] modelKeys = new int[]{sub.getDomain(), sub.getType(), sub.getSubType()};
            String modelName = DeviceModelEnum.resolveName(modelKeys[0], modelKeys[1], modelKeys[2]);
            childDevice = Device.create(sub.getSn(), "", mainDevice.getOrgId(), modelKeys, modelName,
                    mainDevice.getSn(), sub.getDeviceSecret(), sub.getNonce(), sub.getThingVersion(), true);
            Assert.isTrue(deviceMapper.insert(childDevice) > 0, () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));
        } else {
            // 更新子设备
            Device.updateForTopo(childDevice, sub.getDeviceSecret(), sub.getNonce(), sub.getThingVersion(), true);
            Assert.isTrue(deviceMapper.updateById(childDevice) > 0, () -> new DeviceException(DeviceErrorCode.UPDATE_FAILED));
        }
    }
}