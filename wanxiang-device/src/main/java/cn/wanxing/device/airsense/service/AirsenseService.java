package cn.wanxing.device.airsense.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.common.log.ApiLog;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.device.airsense.dto.AirsenseQueryRequest;
import cn.wanxing.device.airsense.entity.AirsenseWarning;
import cn.wanxing.device.airsense.mapper.AirsenseWarningMapper;
import cn.wanxing.device.airsense.message.AirsenseAircraft;
import cn.wanxing.device.airsense.message.AirsenseWarningMessage;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.mqtt.DeviceTopicConst;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AirSense 空域服务：处理 thing/product/{sn}/events 消息（method=airsense_warning，
 * 机场 ADS-B 检测到周边民航飞机时推送），逐条入库并提供查询。
 *
 * <p>该事件带 need_reply=1，处理完必须回 events_reply（result=0），否则设备会重发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class AirsenseService {

    /** 需要回执的标记值（events 消息 need_reply 字段） */
    private static final int NEED_REPLY = 1;

    private final ObjectMapper objectMapper;

    private final AirsenseWarningMapper airsenseWarningMapper;

    private final MqttPublisher mqttPublisher;

    private final UserContext userContext;

    /**
     * 处理一条 AirSense 告警消息：每架航班一条记录入库，need_reply=1 时回执
     *
     * @param sn      主题中的网关设备序列号（机场）
     * @param payload 消息原文（JSON 字符串）
     */
    public void handleWarning(String sn, String payload) {
        // 1.读取消息
        AirsenseWarningMessage message;
        try {
            message = objectMapper.readValue(payload, AirsenseWarningMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("解析 AirSense 告警消息失败 sn={} payload={}", sn, payload, e);
            return;
        }
        if (message.getData() == null || message.getData().isEmpty()) {
            return;
        }

        // 2.逐架航班入库
        for (AirsenseAircraft aircraft : message.getData()) {
            AirsenseWarning warning = AirsenseWarning.create(sn, aircraft);
            Assert.isTrue(airsenseWarningMapper.insert(warning) > 0,
                    () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));
        }

        // 3.设备要求回执时回复 events_reply（result=0 表示已处理，设备不再重发）
        if (Integer.valueOf(NEED_REPLY).equals(message.getNeedReply())) {
            sendEventsReply(sn, message);
        }
    }

    /**
     * AirSense 告警列表：分页 + 按设备/等级筛选。
     * 机构隔离：机构用户只能看到本机构设备的告警（子查询实时关联 sys_device，
     * 不引入冗余字段也没有缓存一致性问题）；平台超管可看全部
     */
    @ApiLog("AirSense 告警列表")
    public MultiResult<AirsenseWarning> listWarnings(AirsenseQueryRequest req) {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<AirsenseWarning> qw = new LambdaQueryWrapper<>();
        if (operator.getOrgId() != null) {
            qw.apply("device_sn IN (SELECT sn FROM sys_device WHERE org_id = {0})", operator.getOrgId());
        }
        qw.eq(req.getDeviceSn() != null && !req.getDeviceSn().isBlank(), AirsenseWarning::getDeviceSn, req.getDeviceSn());
        qw.eq(req.getWarningLevel() != null, AirsenseWarning::getWarningLevel, req.getWarningLevel());
        qw.orderByDesc(AirsenseWarning::getCreatedAt);

        Page<AirsenseWarning> page = airsenseWarningMapper.selectPage(
                new Page<>(req.getCurrentPage(), req.getPageSize()), qw);
        return MultiResult.successMulti(page.getRecords(), page.getTotal(), req.getCurrentPage(), req.getPageSize());
    }

    /**
     * 回复事件回执到 events_reply：回传相同的 tid/bid/method（设备靠它们匹配事件与应答）
     */
    private void sendEventsReply(String sn, AirsenseWarningMessage message) {
        ObjectNode reply = objectMapper.createObjectNode();
        reply.put("tid", message.getTid());
        reply.put("bid", message.getBid());
        reply.put("timestamp", System.currentTimeMillis());
        reply.put("method", message.getMethod());
        reply.putObject("data").put("result", 0);

        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn
                + DeviceTopicConst.EVENTS_SUF + DeviceTopicConst.REPLY_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(reply), "回复 AirSense 告警");
        } catch (JsonProcessingException e) {
            log.error("序列化 events_reply 失败 sn={}", sn, e);
        }
    }
}
