package cn.wanxing.device.live.service;

import cn.wanxing.common.log.ApiLog;
import cn.wanxing.common.log.TraceContext;
import cn.wanxing.device.config.LiveProperties;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.live.dto.LiveCameraChangeRequest;
import cn.wanxing.device.live.dto.LiveLensChangeRequest;
import cn.wanxing.device.live.dto.LiveQualityRequest;
import cn.wanxing.device.live.dto.LiveStartRequest;
import cn.wanxing.device.live.dto.LiveStopRequest;
import cn.wanxing.device.mqtt.DeviceTopicConst;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * 直播服务：下发直播指令（开始/停止/清晰度/镜头/相机切换），处理 services_reply 回执并实时推送前端。
 *
 * <p>直播能力（live_capacity，含可用相机与码流列表）由设备经 state 主题变化推送，
 * 已随设备状态链路存入 sys_device_state.state_json，前端从状态接口读取，本服务不做解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class LiveService {

    /** 直播协议类型（url_type）：1 RTMP / 3 GB28181 / 4 WebRTC（WHIP） */
    private static final int URL_TYPE_RTMP = 1;
    private static final int URL_TYPE_GB28181 = 3;
    private static final int URL_TYPE_WEBRTC = 4;

    /** 直播回执 WebSocket 推送主题前缀（前端订阅 /topic/device/{sn}/live） */
    private static final String LIVE_TOPIC_PREFIX = "/topic/device/";

    private final ObjectMapper objectMapper;

    private final MqttPublisher mqttPublisher;

    private final DeviceMapper deviceMapper;

    private final UserContext userContext;

    private final LiveProperties liveProperties;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 开始直播：下发 live_start_push，直播参数不传时按协议类型从配置兜底生成
     */
    @ApiLog("开始直播")
    public Boolean start(String sn, LiveStartRequest req) {
        // 1.校验设备合法
        checkAccess(sn);

        // 2.组装直播参数
        ObjectNode data = objectMapper.createObjectNode();
        data.put("video_id", req.getVideoId());
        data.put("url_type", req.getUrlType());
        data.put("url", resolveUrl(req.getUrlType(), req.getUrl(), req.getVideoId()));
        data.put("video_quality", req.getVideoQuality());

        // 3.下发
        publishService(sn, "live_start_push", data);
        return Boolean.TRUE;
    }

    /**
     * 停止直播：下发 live_stop_push
     */
    @ApiLog("停止直播")
    public Boolean stop(String sn, LiveStopRequest req) {
        checkAccess(sn);
        ObjectNode data = objectMapper.createObjectNode();
        data.put("video_id", req.getVideoId());
        publishService(sn, "live_stop_push", data);
        return Boolean.TRUE;
    }

    /**
     * 设置直播清晰度：下发 live_set_quality
     */
    @ApiLog("设置直播清晰度")
    public Boolean setQuality(String sn, LiveQualityRequest req) {
        checkAccess(sn);
        ObjectNode data = objectMapper.createObjectNode();
        data.put("video_id", req.getVideoId());
        data.put("video_quality", req.getVideoQuality());
        publishService(sn, "live_set_quality", data);
        return Boolean.TRUE;
    }

    /**
     * 设置直播镜头：下发 live_lens_change（不影响直播进程）
     */
    @ApiLog("设置直播镜头")
    public Boolean changeLens(String sn, LiveLensChangeRequest req) {
        checkAccess(sn);
        ObjectNode data = objectMapper.createObjectNode();
        data.put("video_type", req.getVideoType());
        publishService(sn, "live_lens_change", data);
        return Boolean.TRUE;
    }

    /**
     * 直播相机切换：下发 live_camera_change（Dock 3，FPV 舱内/舱外）
     */
    @ApiLog("直播相机切换")
    public Boolean changeCamera(String sn, LiveCameraChangeRequest req) {
        checkAccess(sn);
        ObjectNode data = objectMapper.createObjectNode();
        if (req.getVideoId() != null && !req.getVideoId().isBlank()) {
            data.put("video_id", req.getVideoId());
        }
        data.put("camera_position", req.getCameraPosition());
        publishService(sn, "live_camera_change", data);
        return Boolean.TRUE;
    }

    /**
     * 处理直播指令回执（services_reply，method=live_*）：result 记日志，并实时推送给订阅该设备的前端
     */
    public void handleReply(String sn, String payload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("解析直播回执失败 sn={} payload={}", sn, payload, e);
            return;
        }
        String method = root.path("method").asText(null);
        int result = root.path("data").path("result").asInt(-1);
        if (result == 0) {
            log.info("直播指令执行成功 sn={} method={}", sn, method);
        } else {
            log.warn("直播指令执行失败 sn={} method={} result={}", sn, method, result);
        }

        // 回执推送给前端：{method, result}
        ObjectNode push = objectMapper.createObjectNode();
        push.put("method", method);
        push.put("result", result);
        try {
            messagingTemplate.convertAndSend(LIVE_TOPIC_PREFIX + sn + "/live",
                    objectMapper.writeValueAsString(push));
        } catch (JsonProcessingException e) {
            log.warn("直播回执推送序列化失败 sn={}", sn, e);
        }
    }

    /**
     * 解析直播参数：显式传入优先；否则按协议类型从 wanxiang.live 配置生成——
     * RTMP/WHIP 的流名取 video_id（/ 替换为 _，同一设备+相机+码流稳定映射，可作拉流地址）；
     * GB28181 参数固定于服务器，取整串配置
     */
    private String resolveUrl(Integer urlType, String url, String videoId) {
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        String stream = videoId.replace('/', '_');
        return switch (urlType) {
            case URL_TYPE_RTMP -> requireConfig(liveProperties.getRtmpUrl()) + "/" + stream;
            case URL_TYPE_WEBRTC -> requireConfig(liveProperties.getWhipUrl()) + "?app=live&stream=" + stream;
            case URL_TYPE_GB28181 -> requireConfig(liveProperties.getGb28181Url());
            default -> throw new DeviceException(DeviceErrorCode.LIVE_URL_MISSING);
        };
    }

    /**
     * 兜底配置为空时直接报错，让调用方在前端就能看到原因而不是等设备超时
     */
    private String requireConfig(String value) {
        if (StringUtils.isBlank(value)) {
            throw new DeviceException(DeviceErrorCode.LIVE_URL_MISSING);
        }
        return value;
    }

    /**
     * 下发一条直播指令到 services 主题：tid 复用 traceId，设备回执原样带回，与本次操作全链路关联
     */
    private void publishService(String sn, String method, ObjectNode data) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("tid", TraceContext.traceIdOrNew());
        message.put("bid", UUID.randomUUID().toString());
        message.put("timestamp", System.currentTimeMillis());
        message.put("method", method);
        message.set("data", data);

        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn + DeviceTopicConst.SERVICES_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(message), "下发直播指令 " + method);
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.LIVE_COMMAND_FAILED);
        }
        log.info("已下发直播指令 sn={} method={}", sn, method);
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
