package cn.wanxing.device.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.common.exception.ErrorCode;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.constant.DeviceModelEnum;
import cn.wanxing.device.constant.DeviceStatusEnum;
import cn.wanxing.device.constant.DeviceTopicConst;
import cn.wanxing.device.dto.SubDeviceInfo;
import cn.wanxing.device.dto.TopologyData;
import cn.wanxing.device.dto.TopologyMessage;
import cn.wanxing.device.dto.request.DeviceQueryRequest;
import cn.wanxing.device.dto.vo.DeviceVO;
import cn.wanxing.device.entity.Device;
import cn.wanxing.device.entity.DeviceState;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.mapper.DeviceMapper;
import cn.wanxing.device.mapper.DeviceStateMapper;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.Org;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.mapper.OrgMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备服务：负责「设备列表」与「设备上下线状态维护」。
 *
 * <p>设备通过「绑定码」在 MQTT 绑定流程中写入机构（见 {@link DockRequestService}），
 * 本服务在设备上下线时按 SN 匹配已绑定记录更新在线状态，未绑定的设备消息直接忽略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;

    private final DeviceStateMapper deviceStateMapper;

    private final ObjectMapper objectMapper;

    private final UserContext userContext;

    private final OrgMapper orgMapper;

    private final MqttPublisher mqttPublisher;

    /**
     * 设备列表：分页 + 筛选。机构管理员固定看本机构，平台超管可按组织/型号/状态/关键字筛选。
     */
    public MultiResult<DeviceVO> list(DeviceQueryRequest req) {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<>();

        // 1.机构隔离：机构管理员固定看本机构；平台超管可按 orgId 筛选
        if (operator.getOrgId() != null) {
            qw.eq(Device::getOrgId, operator.getOrgId());
        } else if (req.getOrgId() != null) {
            qw.eq(Device::getOrgId, req.getOrgId());
        }

        // 2.设备型号筛选（domain / type / sub_type）
        qw.eq(req.getDomain() != null, Device::getDomain, req.getDomain())
                .eq(req.getType() != null, Device::getType, req.getType())
                .eq(req.getSubType() != null, Device::getSubType, req.getSubType());

        // 3.在线状态筛选
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try {
                qw.eq(Device::getStatus, DeviceStatusEnum.valueOf(req.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // 非法状态值，忽略该筛选
            }
        }

        // 4.关键字模糊搜索（SN 或名称）
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            qw.and(w -> w.like(Device::getSn, req.getKeyword())
                    .or().like(Device::getName, req.getKeyword()));
        }

        // 5.分页查询
        Page<Device> page = deviceMapper.selectPage(new Page<>(req.getCurrentPage(), req.getPageSize()), qw);
        List<Device> devices = page.getRecords();
        if (devices.isEmpty()) {
            return MultiResult.successMulti(Collections.emptyList(), page.getTotal(),
                    req.getCurrentPage(), req.getPageSize());
        }

        // 6.批量加载机构名，避免逐条查询（N+1 问题）
        Set<Long> orgIds = devices.stream()
                .map(Device::getOrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Org> orgMap = orgIds.isEmpty()
                ? Collections.emptyMap()
                : orgMapper.selectBatchIds(orgIds).stream()
                        .collect(Collectors.toMap(Org::getId, Function.identity()));

        // 7.组装 VO，填充机构名
        List<DeviceVO> vos = devices.stream().map(device -> {
            DeviceVO vo = DeviceVO.from(device);
            Org org = orgMap.get(device.getOrgId());
            if (org != null) {
                vo.setOrgName(org.getName());
            }
            return vo;
        }).toList();

        return MultiResult.successMulti(vos, page.getTotal(), req.getCurrentPage(), req.getPageSize());
    }

    // ============ 设备管理 ============

    /**
     * 设备详情：按 SN 查询单台设备（含机构名），机构隔离
     */
    public DeviceVO detail(String sn) {
        Device device = getDeviceBySn(sn);
        DeviceVO vo = DeviceVO.from(device);
        if (device.getOrgId() != null) {
            Org org = orgMapper.selectById(device.getOrgId());
            if (org != null) {
                vo.setOrgName(org.getName());
            }
        }
        return vo;
    }

    /**
     * 设备最新状态：查询 sys_device_state 里该设备最近一条 state
     */
    public DeviceState getState(String sn) {
        // 校验设备存在且当前用户有权限（机构隔离）
        getDeviceBySn(sn);
        return deviceStateMapper.selectOne(
                new LambdaQueryWrapper<DeviceState>().eq(DeviceState::getDeviceSn, sn));
    }

    /**
     * 解绑设备：把设备从机构移除（org_id 置空，绑定时间清空）
     */
    public void unbind(String sn) {
        Device device = getDeviceBySn(sn);
        device.setOrgId(null);
        device.setBoundAt(null);
        deviceMapper.updateById(device);
    }

    /**
     * 重命名设备
     */
    public void rename(String sn, String name) {
        Device device = getDeviceBySn(sn);
        device.setName(name);
        deviceMapper.updateById(device);
    }

    /**
     * 按 SN 查询设备，并校验当前用户是否有权访问（平台超管可访问全部，机构用户只能访问本机构）
     */
    private Device getDeviceBySn(String sn) {
        User operator = userContext.currentUser();
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (device == null) {
            throw new DeviceException(DeviceErrorCode.DEVICE_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), device.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        return device;
    }

    // ============ MQTT 上下线处理 ============

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

        // 6.回 status_reply 确认（协议要求云端收到拓扑更新后回执 result=0）
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

    // ============ 私有方法 ============

    /**
     * 处理子设备上线（如机场内的无人机）：按 SN 查询，不存在则新建，并继承父设备的机构
     */
    private void upsertChild(Device mainDevice, SubDeviceInfo sub) {
        // 1.按 SN 查子设备，不存在则新建
        Device childDevice = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sub.getSn()));
        boolean isNew = childDevice == null;
        if (isNew) {
            // 新建子设备
            int[] modelKeys = new int[]{sub.getDomain(),sub.getType(),sub.getSubType()};
            String modelName = DeviceModelEnum.resolveName(modelKeys[0], modelKeys[1], modelKeys[2]);
            childDevice = Device.create(sub.getSn(), "", mainDevice.getOrgId(), modelKeys, modelName, mainDevice.getSn(), sub.getDeviceSecret(), sub.getNonce(), sub.getThingVersion(), true);
            Assert.isTrue(deviceMapper.insert(childDevice) > 0, () -> new  DeviceException(DeviceErrorCode.INSERT_FAILED));
        }else {
            // 更新子设备
            Device.updateForTopo(childDevice, sub.getDeviceSecret(), sub.getNonce(), sub.getThingVersion(), true);
            Assert.isTrue(deviceMapper.updateById(childDevice) > 0, () -> new  DeviceException(DeviceErrorCode.UPDATE_FAILED));
        }
    }

}
