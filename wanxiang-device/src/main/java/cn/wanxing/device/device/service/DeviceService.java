package cn.wanxing.device.device.service;

import cn.wanxing.common.log.ApiLog;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.dto.DeviceQueryRequest;
import cn.wanxing.device.device.dto.DeviceVO;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.device.constant.DeviceStatusEnum;
import cn.wanxing.device.status.entity.DeviceState;
import cn.wanxing.device.status.mapper.DeviceStateMapper;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.Org;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.mapper.OrgMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备服务：负责「设备列表/详情/解绑/重命名」等设备管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;

    private final DeviceStateMapper deviceStateMapper;

    private final UserContext userContext;

    private final OrgMapper orgMapper;

    /**
     * 设备列表：分页 + 筛选。机构管理员固定看本机构，平台超管可按组织/型号/状态/关键字筛选。
     */
    @ApiLog("设备列表")
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

    /**
     * 设备详情：按 SN 查询单台设备（含机构名），机构隔离
     */
    @ApiLog("设备详情")
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
    @ApiLog("设备状态")
    public DeviceState getState(String sn) {
        // 校验设备存在且当前用户有权限（机构隔离）
        getDeviceBySn(sn);
        return deviceStateMapper.selectOne(
                new LambdaQueryWrapper<DeviceState>().eq(DeviceState::getDeviceSn, sn));
    }

    /**
     * 解绑设备：把设备从机构移除（org_id 置空，绑定时间清空）
     */
    @ApiLog("解绑设备")
    public void unbind(String sn) {
        Device device = getDeviceBySn(sn);
        device.setOrgId(null);
        device.setBoundAt(null);
        deviceMapper.updateById(device);
    }

    /**
     * 重命名设备
     */
    @ApiLog("重命名设备")
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
}