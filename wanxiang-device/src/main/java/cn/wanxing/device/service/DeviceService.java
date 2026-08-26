package cn.wanxing.device.service;

import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.constant.DeviceModelEnum;
import cn.wanxing.device.constant.DeviceStatusEnum;
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
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.Org;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.mapper.OrgMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        // 2.判断在线还是离线：sub_devices 非空 = 上线，空/null = 离线
        boolean online = data.getSubDevices() != null && !data.getSubDevices().isEmpty();
        LocalDateTime now = LocalDateTime.now();

        // 3.主设备必须是已绑定设备，否则忽略（未绑定设备不追踪）
        Device main = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        if (main == null) {
            log.warn("收到未绑定设备的上下线消息，已忽略 sn={}", sn);
            return;
        }

        // 4.更新主设备状态（在线时记录最近上线时间）
        main.setDomain(data.getDomain());
        main.setType(data.getType());
        main.setSubType(data.getSubType());
        main.setModelName(DeviceModelEnum.resolveName(data.getDomain(), data.getType(), data.getSubType()));
        main.setStatus(online ? DeviceStatusEnum.ONLINE : DeviceStatusEnum.OFFLINE);
        if (online) {
            main.setLastOnlineAt(now);
        }
        deviceMapper.updateById(main);

        // 5.处理子设备：上线时挂到主设备下（继承机构），离线时一并置离线
        if (online) {
            for (SubDeviceInfo sub : data.getSubDevices()) {
                upsertChild(sub.getSn(), sn, main.getOrgId(), sub.getDomain(),
                        sub.getType(), sub.getSubType(), sub.getIndex(), now);
            }
        } else {
            List<Device> children = deviceMapper.selectList(
                    new LambdaQueryWrapper<Device>().eq(Device::getParentSn, sn));
            for (Device child : children) {
                child.setStatus(DeviceStatusEnum.OFFLINE);
                deviceMapper.updateById(child);
            }
        }
    }

    // ============ 私有方法 ============

    /**
     * 子设备（如机场内的无人机）：按 SN 查询，不存在则新建，并继承父设备的机构
     */
    private void upsertChild(String sn, String parentSn, Long orgId, Integer domain, Integer type, Integer subType, String index, LocalDateTime now) {
        // 1.按 SN 查子设备，不存在则新建
        Device child = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
        boolean isNew = child == null;
        if (isNew) {
            child = new Device();
            child.setSn(sn);
        }

        // 2.设置父子关系、所属机构（继承父设备）、设备域/型号、在线状态
        child.setParentSn(parentSn);
        child.setOrgId(orgId);
        child.setDomain(domain);
        child.setType(type);
        child.setSubType(subType);
        child.setDeviceIndex(index);
        child.setModelName(DeviceModelEnum.resolveName(domain, type, subType));
        child.setStatus(DeviceStatusEnum.ONLINE);
        child.setLastOnlineAt(now);

        // 3.插入或更新
        if (isNew) {
            deviceMapper.insert(child);
        } else {
            deviceMapper.updateById(child);
        }
    }

}
