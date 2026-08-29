package cn.wanxing.device.alarm.service;

import cn.wanxing.common.log.ApiLog;
import cn.hutool.core.lang.Assert;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.alarm.message.HmsAlarm;
import cn.wanxing.device.alarm.message.HmsData;
import cn.wanxing.device.alarm.entity.HmsDictionary;
import cn.wanxing.device.alarm.message.HmsMessage;
import cn.wanxing.device.alarm.dto.AlarmQueryRequest;
import cn.wanxing.device.alarm.entity.Alarm;
import cn.wanxing.device.alarm.mapper.AlarmMapper;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 设备事件服务：处理 thing/product/{sn}/events 消息（健康告警 HMS），并提供告警查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final ObjectMapper objectMapper;

    private final AlarmMapper alarmMapper;

    private final HmsDictionary hmsDictionary;

    private final UserContext userContext;

    /**
     * 处理一条 events 消息：目前只处理 method=hms 的健康告警
     *
     * @param sn      主题中的设备序列号
     * @param payload 消息原文（JSON 字符串）
     */
    public void handleEvents(String sn, String payload) {
        // 1.读取消息
        HmsMessage message;
        try {
            message = objectMapper.readValue(payload, HmsMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("解析设备事件消息失败 sn={} payload={}", sn, payload, e);
            return;
        }
        if (!"hms".equals(message.getMethod())) {
            log.debug("暂不支持的事件 method={} sn={}", message.getMethod(), sn);
            return;
        }

        // 2.获取data
        HmsData data = message.getData();
        if (data == null || data.getList() == null || data.getList().isEmpty()) {
            return;
        }

        // 3.新增alarm
        for (HmsAlarm hmsAlarm : data.getList()) {
            Alarm alarm = Alarm.create(hmsAlarm, sn, hmsDictionary);
            Assert.isTrue(alarmMapper.insert(alarm) > 0, () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));
        }
    }

    /**
     * 告警列表：分页 + 按设备/等级筛选。
     * 机构隔离：机构用户只能看到本机构设备的告警（子查询实时关联 sys_device，
     * 不引入冗余字段也没有缓存一致性问题）；平台超管可看全部
     */
    @ApiLog("告警列表")
    public MultiResult<Alarm> listAlarms(AlarmQueryRequest req) {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<Alarm> qw = new LambdaQueryWrapper<>();
        if (operator.getOrgId() != null) {
            qw.apply("device_sn IN (SELECT sn FROM sys_device WHERE org_id = {0})", operator.getOrgId());
        }
        qw.eq(req.getDeviceSn() != null && !req.getDeviceSn().isBlank(), Alarm::getDeviceSn, req.getDeviceSn());
        qw.eq(req.getLevel() != null, Alarm::getLevel, req.getLevel());
        qw.orderByDesc(Alarm::getCreatedAt);

        Page<Alarm> page = alarmMapper.selectPage(new Page<>(req.getCurrentPage(), req.getPageSize()), qw);
        return MultiResult.successMulti(page.getRecords(), page.getTotal(), req.getCurrentPage(), req.getPageSize());
    }
}