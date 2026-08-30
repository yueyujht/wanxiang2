package cn.wanxing.device.wayline.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.common.log.ApiLog;
import cn.wanxing.common.log.TraceContext;
import cn.wanxing.device.config.WaylineProperties;
import cn.wanxing.device.device.entity.Device;
import cn.wanxing.device.device.mapper.DeviceMapper;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.mqtt.DeviceTopicConst;
import cn.wanxing.device.mqtt.MqttPublisher;
import cn.wanxing.device.wayline.dto.WaylineJobCreateRequest;
import cn.wanxing.device.wayline.entity.WaylineFile;
import cn.wanxing.device.wayline.entity.WaylineJob;
import cn.wanxing.device.wayline.mapper.WaylineFileMapper;
import cn.wanxing.device.wayline.mapper.WaylineJobMapper;
import cn.wanxing.device.wayline.message.WaylineProgressMessage;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 航线任务服务：任务创建与生命周期（prepare → execute → progress → 终态）、取消、资源获取。
 *
 * <p>任务状态机（status 字段）：
 * pending（创建未下发）→ [prepare] sent（已下发待执行）→ [execute] executing（执行中）
 * → 终态 ok/failed/canceled/timeout/partially_done（由设备 progress.status 权威驱动）。
 *
 * <p>调度设计（DB 驱动，应用重启任务不丢）：扫描器每 30 秒执行两步——
 * ① pending 且距执行时间 ≤24h 的任务补发 prepare（官方允许最早提前 24h 下发）；
 * ② prepare 完成且距执行时间 ≤2min 的任务下发 execute（官方执行时机）。
 * 立即任务在创建请求内同步 prepare（30 秒设备校验容忍度，不等扫描器）。
 * sent/executing 超过 30 分钟无任何进度更新判 timeout（设备断电/失联兜底）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class WaylineJobService {

    /** 任务类型：0 立即 / 1 定时（2 条件任务二期） */
    private static final int TASK_TYPE_IMMEDIATE = 0;
    private static final int TASK_TYPE_SCHEDULED = 1;

    /** 任务状态（平台侧流转 + 设备 progress 权威状态） */
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_SENT = "sent";
    private static final String STATUS_EXECUTING = "executing";
    private static final String STATUS_CANCELED = "canceled";
    private static final String STATUS_TIMEOUT = "timeout";

    /** 定时任务最早提前下发 prepare 的时间窗（官方语义 24h） */
    private static final long PREPARE_ADVANCE_MS = 24 * 3600_000L;
    /** 执行指令提前量（官方语义：执行时间前 2 分钟下发 execute） */
    private static final long EXECUTE_ADVANCE_MS = 2 * 60_000L;
    /** sent/executing 无进度更新超过该时长判 timeout */
    private static final long TIMEOUT_NO_PROGRESS_MS = 30 * 60_000L;

    /** WebSocket 推送主题：任务进度 / 指令回执 */
    private static final String PROGRESS_TOPIC_PREFIX = "/topic/device/";
    private static final String PROGRESS_TOPIC_SUFFIX = "/wayline/progress";
    private static final String REPLY_TOPIC_SUFFIX = "/wayline/reply";

    private final ObjectMapper objectMapper;

    private final WaylineJobMapper waylineJobMapper;

    private final WaylineFileMapper waylineFileMapper;

    private final DeviceMapper deviceMapper;

    private final MqttPublisher mqttPublisher;

    private final UserContext userContext;

    private final WaylineProperties waylineProperties;

    private final SimpMessagingTemplate messagingTemplate;

    // ============ 任务管理（HTTP） ============

    /**
     * 创建任务：校验文件与机构归属后落库（pending）；立即任务同步下发 prepare
     * （设备对 execute_time 有 30 秒校验容忍度，不等扫描器），定时任务交给调度器
     */
    @ApiLog("创建航线任务")
    public WaylineJob createJob(String sn, WaylineJobCreateRequest req) {
        // 1.校验设备合法
        Device device = checkAccess(sn);
        // 2.校验航线文件：机构用户可用本机构 + 全局文件
        User operator = userContext.currentUser();
        WaylineFile file = waylineFileMapper.selectById(req.getWaylineFileId());
        if (file == null) {
            throw new DeviceException(DeviceErrorCode.WAYLINE_FILE_NOT_FOUND);
        }
        if (operator.getOrgId() != null
                && file.getOrgId() != null && !Objects.equals(operator.getOrgId(), file.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        // 3.模拟器任务必须给起始坐标
        if (Boolean.TRUE.equals(req.getSimulateMission())
                && (req.getSimulateLatitude() == null || req.getSimulateLongitude() == null)) {
            throw new DeviceException(DeviceErrorCode.PROPERTY_VALUE_INVALID);
        }

        // 4.落库（pending），org_id 取目标设备机构（机构用户创建时两者相同）
        WaylineJob job = WaylineJob.create(sn, device.getOrgId(), file.getId(), req.getName(),
                req.getTaskType(), req.getExecuteTime(), new WaylineJob.WaylineJobParams(
                        req.getRthAltitude(), req.getExitWaylineWhenRcLost(), req.getWaylinePrecisionType(),
                        req.getSimulateMission(), req.getSimulateLatitude(), req.getSimulateLongitude(),
                        req.getStorageCapacity()));
        Assert.isTrue(waylineJobMapper.insert(job) > 0, () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));

        // 5.立即任务同步下发 prepare（设备 30 秒校验容忍度）
        if (req.getTaskType() == TASK_TYPE_IMMEDIATE) {
            sendPrepare(job, file);
            job.setPrepareSent(true);
            job.setStatus(STATUS_SENT);
            waylineJobMapper.updateById(job);
        }
        return job;
    }

    /**
     * 任务列表：分页 + 按设备/状态筛选，机构隔离
     */
    @ApiLog("航线任务列表")
    public List<WaylineJob> listJobs(String deviceSn, String status) {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<WaylineJob> qw = new LambdaQueryWrapper<>();
        if (operator.getOrgId() != null) {
            qw.eq(WaylineJob::getOrgId, operator.getOrgId());
        }
        qw.eq(StringUtils.isNotBlank(deviceSn), WaylineJob::getDeviceSn, deviceSn);
        qw.eq(StringUtils.isNotBlank(status), WaylineJob::getStatus, status);
        qw.orderByDesc(WaylineJob::getId);
        return waylineJobMapper.selectList(qw);
    }

    /**
     * 取消任务：官方语义"仅能取消任务的下发，无法取消正在执行中的任务"——
     * pending 本地直接取消（设备侧无感知）；sent 下发 flighttask_undo 后取消；
     * executing/终态拒绝
     */
    @ApiLog("取消航线任务")
    public Boolean cancel(Long id) {
        User operator = userContext.currentUser();
        WaylineJob job = waylineJobMapper.selectById(id);
        if (job == null) {
            throw new DeviceException(DeviceErrorCode.WAYLINE_JOB_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), job.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        if (STATUS_EXECUTING.equals(job.getStatus())
                || isTerminal(job.getStatus())) {
            throw new DeviceException(DeviceErrorCode.WAYLINE_JOB_STATE_INVALID);
        }
        // 已 prepare 的任务需通知设备撤销
        if (Boolean.TRUE.equals(job.getPrepareSent())) {
            ObjectNode data = objectMapper.createObjectNode();
            ArrayNode flightIds = data.putArray("flight_ids");
            flightIds.add(job.getFlightId());
            publishService(job.getDeviceSn(), "flighttask_undo", data);
        }
        job.setStatus(STATUS_CANCELED);
        Assert.isTrue(waylineJobMapper.updateById(job) > 0, () -> new DeviceException(DeviceErrorCode.UPDATE_FAILED));
        return Boolean.TRUE;
    }

    // ============ 调度（prepare/execute 到点下发） ============

    /**
     * 任务调度扫描：① 补发 prepare（≤24h 窗口）② 到点下发 execute（≤2min 窗口）
     * ③ sent/executing 无进度超时判 timeout。全部 DB 驱动，应用重启不丢任务
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void scanJobs() {
        long now = System.currentTimeMillis();
        // ① pending → prepare（执行时间已进入 24h 窗口）
        for (WaylineJob job : waylineJobMapper.selectList(new LambdaQueryWrapper<WaylineJob>()
                .eq(WaylineJob::getStatus, STATUS_PENDING)
                .eq(WaylineJob::getPrepareSent, false)
                .le(WaylineJob::getExecuteTime, now + PREPARE_ADVANCE_MS))) {
            WaylineFile file = waylineFileMapper.selectById(job.getWaylineFileId());
            if (file == null) {
                log.warn("调度跳过：任务引用的航线文件不存在 id={} flightId={}", job.getId(), job.getFlightId());
                continue;
            }
            try {
                sendPrepare(job, file);
                job.setPrepareSent(true);
                job.setStatus(STATUS_SENT);
                waylineJobMapper.updateById(job);
            } catch (Exception e) {
                // MQTT 异常等下轮扫描重试
                log.error("下发任务 prepare 失败，下轮重试 flightId={}", job.getFlightId(), e);
            }
        }
        // ② sent → execute（进入执行前 2 分钟窗口）
        for (WaylineJob job : waylineJobMapper.selectList(new LambdaQueryWrapper<WaylineJob>()
                .eq(WaylineJob::getStatus, STATUS_SENT)
                .eq(WaylineJob::getPrepareSent, true)
                .eq(WaylineJob::getExecuteSent, false)
                .le(WaylineJob::getExecuteTime, now + EXECUTE_ADVANCE_MS))) {
            try {
                sendExecute(job);
                job.setExecuteSent(true);
                job.setStatus(STATUS_EXECUTING);
                waylineJobMapper.updateById(job);
            } catch (Exception e) {
                log.error("下发任务 execute 失败，下轮重试 flightId={}", job.getFlightId(), e);
            }
        }
        // ③ 超时兜底：sent/executing 长时间无进度更新（update_time 由每次进度写入刷新，见 handleProgress）
        LocalDateTime timeoutBefore = LocalDateTime.now().minusSeconds(TIMEOUT_NO_PROGRESS_MS / 1000);
        for (WaylineJob job : waylineJobMapper.selectList(new LambdaQueryWrapper<WaylineJob>()
                .in(WaylineJob::getStatus, Arrays.asList(STATUS_SENT, STATUS_EXECUTING))
                .lt(WaylineJob::getUpdatedAt, timeoutBefore))) {
            job.setStatus(STATUS_TIMEOUT);
            waylineJobMapper.updateById(job);
            log.warn("任务无进度更新超时，判 timeout flightId={} sn={}", job.getFlightId(), job.getDeviceSn());
        }
    }

    // ============ MQTT 入站处理 ============

    /**
     * 处理任务进度事件（events: flighttask_progress）：按 flight_id 更新任务
     * （状态/步骤/百分比/媒体数量/断点），推送前端，need_reply=1 时回执
     */
    public void handleProgress(String sn, String payload) {
        WaylineProgressMessage message;
        try {
            message = objectMapper.readValue(payload, WaylineProgressMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("解析航线任务进度失败 sn={} payload={}", sn, payload, e);
            return;
        }
        if (message.getData() == null || message.getData().getOutput() == null) {
            return;
        }
        WaylineProgressMessage.Output output = message.getData().getOutput();
        WaylineProgressMessage.Ext ext = output.getExt();
        String flightId = ext == null ? null : ext.getFlightId();
        if (flightId == null) {
            log.warn("航线任务进度缺少 flight_id，忽略 sn={}", sn);
            return;
        }
        WaylineProgressMessage.Progress progress = output.getProgress();
        WaylineJob job = waylineJobMapper.selectOne(
                new LambdaQueryWrapper<WaylineJob>().eq(WaylineJob::getFlightId, flightId));
        if (job == null) {
            log.warn("收到未建档任务的进度，已忽略 sn={} flightId={} status={}", sn, flightId, output.getStatus());
            return;
        }
        // 断点原文入库（二期断点续飞的依据）
        String breakpointJson = ext.getBreakPoint() == null ? null : writeJson(ext.getBreakPoint());
        job.applyProgress(output.getStatus(), progress == null ? null : progress.getCurrentStep(),
                progress == null ? null : progress.getPercent(), ext.getMediaCount(), breakpointJson);
        // 显式推进 update_time：实体未挂自动填充，updateById 会把查出的旧值原样写回，
        // 不刷新则超时兜底（按 update_time 判定）会把执行中的长任务误判 timeout
        job.setUpdatedAt(java.time.LocalDateTime.now());
        waylineJobMapper.updateById(job);

        log.info("航线任务进度 sn={} flightId={} status={} percent={}",
                sn, flightId, output.getStatus(), progress == null ? null : progress.getPercent());
        ObjectNode push = objectMapper.createObjectNode();
        push.put("flightId", flightId);
        push.put("status", output.getStatus());
        push.put("percent", progress == null ? null : progress.getPercent());
        push.put("currentStep", progress == null ? null : progress.getCurrentStep());
        push.put("mediaCount", ext.getMediaCount());
        pushProgress(sn, push);
        if (Integer.valueOf(1).equals(message.getNeedReply())) {
            sendEventsReply(sn, message.getTid(), message.getBid(), message.getMethod());
        }
    }

    /**
     * 处理任务指令回执（services_reply: flighttask_prepare/execute/undo）：记日志并推送前端。
     * 任务状态不在此处流转（prepare/execute 的下发成功已在调度时落库，执行状态以 progress 为权威）；
     * 回执 result 非 0 时设备侧拒绝，由超时兜底或 progress 中断状态收敛
     */
    public void handleReply(String sn, String payload) {
        String method = null;
        int result = -1;
        try {
            JsonNode root = objectMapper.readTree(payload);
            method = root.path("method").asText(null);
            result = root.path("data").path("result").asInt(-1);
        } catch (JsonProcessingException e) {
            log.warn("解析航线任务回执失败 sn={} payload={}", sn, payload, e);
            return;
        }
        if (result == 0) {
            log.info("航线任务指令回执成功 sn={} method={}", sn, method);
        } else {
            log.warn("航线任务指令回执失败 sn={} method={} result={}", sn, method, result);
        }
        ObjectNode push = objectMapper.createObjectNode();
        push.put("method", method);
        push.put("result", result);
        try {
            messagingTemplate.convertAndSend(PROGRESS_TOPIC_PREFIX + sn + REPLY_TOPIC_SUFFIX,
                    objectMapper.writeValueAsString(push));
        } catch (JsonProcessingException e) {
            log.warn("航线任务回执推送序列化失败 sn={}", sn, e);
        }
    }

    /**
     * 组装 flighttask_resource_get 的文件信息（requests: 任务资源获取）：
     * {file: {url, fingerprint}}；任务或文件缺失时回空对象并告警（设备侧会以进度 failed 收敛）
     */
    public ObjectNode buildResourceOutput(String flightId) {
        ObjectNode output = objectMapper.createObjectNode();
        WaylineJob job = flightId == null ? null : waylineJobMapper.selectOne(
                new LambdaQueryWrapper<WaylineJob>().eq(WaylineJob::getFlightId, flightId));
        WaylineFile file = job == null ? null : waylineFileMapper.selectById(job.getWaylineFileId());
        if (job == null || file == null) {
            log.warn("任务资源获取失败：任务或航线文件不存在 flightId={}", flightId);
            return output;
        }
        ObjectNode fileNode = output.putObject("file");
        fileNode.put("url", buildDownloadUrl(file.getId()));
        fileNode.put("fingerprint", file.getFingerprint());
        return output;
    }

    /**
     * 查询设备当前进行中任务的 flight_id（媒体上传优先级用，兑现媒体模块遗留 TODO）
     */
    public String findActiveFlightId(String sn) {
        WaylineJob job = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJob>()
                .eq(WaylineJob::getDeviceSn, sn)
                .in(WaylineJob::getStatus, Arrays.asList(STATUS_SENT, STATUS_EXECUTING))
                .orderByDesc(WaylineJob::getId)
                .last("LIMIT 1"));
        return job == null ? null : job.getFlightId();
    }

    // ============ 内部辅助 ============

    /**
     * 下发任务（services: flighttask_prepare）：字段与官方协议一一对应，
     * 条件任务与断点续飞字段为二期，暂不组装
     */
    private void sendPrepare(WaylineJob job, WaylineFile file) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("flight_id", job.getFlightId());
        data.put("execute_time", job.getExecuteTime());
        data.put("task_type", job.getTaskType());
        ObjectNode fileNode = data.putObject("file");
        fileNode.put("url", buildDownloadUrl(file.getId()));
        fileNode.put("fingerprint", file.getFingerprint());
        data.put("rth_altitude", job.getRthAltitude());
        data.put("rth_mode", job.getRthMode());
        data.put("out_of_control_action", job.getOutOfControlAction());
        data.put("exit_wayline_when_rc_lost", job.getExitWaylineWhenRcLost());
        data.put("wayline_precision_type", job.getWaylinePrecisionType());
        if (Boolean.TRUE.equals(job.getSimulateMission())) {
            ObjectNode simulate = data.putObject("simulate_mission");
            simulate.put("is_enable", 1);
            simulate.put("latitude", job.getSimulateLatitude());
            simulate.put("longitude", job.getSimulateLongitude());
        }
        if (job.getStorageCapacity() != null) {
            data.putObject("executable_conditions").put("storage_capacity", job.getStorageCapacity());
        }
        publishService(job.getDeviceSn(), "flighttask_prepare", data);
    }

    /**
     * 下发执行（services: flighttask_execute）：data 仅 flight_id（蛙跳任务参数为二期）
     */
    private void sendExecute(WaylineJob job) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("flight_id", job.getFlightId());
        publishService(job.getDeviceSn(), "flighttask_execute", data);
    }

    private void publishService(String sn, String method, ObjectNode data) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("tid", TraceContext.traceIdOrNew());
        message.put("bid", UUID.randomUUID().toString());
        message.put("timestamp", System.currentTimeMillis());
        message.put("method", method);
        message.set("data", data);

        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn + DeviceTopicConst.SERVICES_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(message), "下发航线任务指令 " + method);
        } catch (JsonProcessingException e) {
            throw new DeviceException(DeviceErrorCode.WAYLINE_COMMAND_FAILED);
        }
        log.info("已下发航线任务指令 sn={} method={} flightId={}", sn, method,
                message.get("data") == null ? null : data.path("flight_id").asText(null));
    }

    private String buildDownloadUrl(Long fileId) {
        String baseUrl = waylineProperties.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            log.error("航线下载基地址未配置（wanxiang.wayline.base-url），设备将无法下载航线文件");
            return "";
        }
        return baseUrl + "/device/wayline/files/" + fileId + "/download";
    }

    private String writeJson(Object node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void pushProgress(String sn, ObjectNode push) {
        try {
            messagingTemplate.convertAndSend(PROGRESS_TOPIC_PREFIX + sn + PROGRESS_TOPIC_SUFFIX,
                    objectMapper.writeValueAsString(push));
        } catch (JsonProcessingException e) {
            log.warn("航线任务进度推送序列化失败 sn={}", sn, e);
        }
    }

    /**
     * 回复事件回执到 events_reply：回传相同的 tid/bid/method（设备靠它们匹配事件与应答）
     */
    private void sendEventsReply(String sn, String tid, String bid, String method) {
        ObjectNode reply = objectMapper.createObjectNode();
        reply.put("tid", tid);
        reply.put("bid", bid);
        reply.put("timestamp", System.currentTimeMillis());
        reply.put("method", method);
        reply.putObject("data").put("result", 0);

        String topic = DeviceTopicConst.THING_PRE + DeviceTopicConst.PRODUCT + sn
                + DeviceTopicConst.EVENTS_SUF + DeviceTopicConst.REPLY_SUF;
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(reply), "回复航线任务进度");
        } catch (JsonProcessingException e) {
            log.error("序列化 events_reply 失败 sn={}", sn, e);
        }
    }

    private Device checkAccess(String sn) {
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

    private boolean isTerminal(String status) {
        return Arrays.asList("ok", "failed", "canceled", "timeout", "partially_done", "rejected").contains(status);
    }
}
