package cn.wanxing.device.mqtt;

/**
 * MQTT 消息场景：把「主题类型 + method」翻译成业务场景中文名，用于收发日志的统一标注，
 * 排查问题时不用再对着 topic 后缀猜业务含义。
 */
public enum MqttScenario {

    /** sys/product/{sn}/status：网关上报拓扑，设备上下线 */
    ONLINE_OFFLINE("设备上下线"),

    /** thing/product/{sn}/osd：0.5Hz 实时遥测 */
    OSD("OSD 遥测"),

    /** thing/product/{sn}/state：增量状态 */
    STATE("设备状态变更"),

    /** thing/product/{sn}/property/set_reply：属性设置回执 */
    PROPERTY_SET_REPLY("属性设置回执"),

    /** events：HMS 健康告警 */
    ALARM_HMS("HMS 健康告警"),

    /** events：固件升级进度 */
    FIRMWARE_PROGRESS("固件升级进度"),

    /** events：远程日志上传进度 */
    REMOTE_LOG_PROGRESS("远程日志上传进度"),

    /** events：AirSense 空域告警（ADS-B 周边航班） */
    AIRSENSE_WARNING("AirSense 空域告警"),

    /** services_reply：ota_create 回执 */
    FIRMWARE_REPLY("固件升级回执"),

    /** services_reply：fileupload_* 回执 */
    REMOTE_LOG_REPLY("远程日志回执"),

    /** services_reply：live_* 直播指令回执 */
    LIVE_REPLY("直播回执"),

    /** requests：config（License 校验） */
    LICENSE_CONFIG("License 配置请求"),

    /** requests：airport_bind_status */
    BIND_STATUS("绑定状态查询"),

    /** requests：airport_organization_get */
    ORG_GET("机构绑定码查询"),

    /** requests：airport_organization_bind */
    ORG_BIND("设备绑定机构"),

    /** requests：storage_config_get（媒体上传临时凭证） */
    STORAGE_CONFIG("存储凭证请求"),

    /** events：媒体文件上传结果 */
    MEDIA_UPLOAD_CALLBACK("媒体上传结果"),

    /** events：媒体高优先级任务查询 */
    MEDIA_PRIORITY_QUERY("媒体优先级查询"),

    /** events：自定义飞行区文件同步进度 */
    FLIGHT_AREAS_SYNC_PROGRESS("飞行区同步进度"),

    /** events：自定义飞行区告警（飞行器位置） */
    FLIGHT_AREAS_WARNING("飞行区告警"),

    /** services_reply：flight_areas_update 飞行区同步回执 */
    FLIGHT_AREAS_REPLY("飞行区同步回执"),

    /** services_reply：flighttask_* 航线任务回执 */
    WAYLINE_REPLY("航线任务回执"),

    /** events：航线任务进度 */
    WAYLINE_PROGRESS("航线任务进度"),

    /** requests：flighttask_resource_get 任务资源获取 */
    WAYLINE_RESOURCE_GET("任务资源获取"),

    /** events：fly-to 执行进度 */
    FLY_TO_PROGRESS("fly-to 进度"),

    /** events：一键起飞进度 */
    TAKEOFF_PROGRESS("一键起飞进度"),

    /** events：DRC 链路状态 */
    DRC_STATUS("DRC 链路状态"),

    /** DRC 高频通道上行透传 */
    DRC_UP("DRC 高频数据"),

    /** 无法归类的消息（未知主题或未知 method） */
    UNKNOWN("未知消息");

    private final String label;

    MqttScenario(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * 按主题类型 + method 解析场景；解析不出的返回 {@link #UNKNOWN}（调用方日志里会带原始 method）
     */
    public static MqttScenario of(DeviceTopicType type, String method) {
        return switch (type) {
            case ONLINE_OFFLINE -> ONLINE_OFFLINE;
            case OSD -> OSD;
            case STATE -> STATE;
            case PROPERTY_SET_REPLY -> PROPERTY_SET_REPLY;
            case DRC_UP -> DRC_UP;
            case REQUESTS -> ofRequest(method);
            case EVENTS -> ofEvent(method);
            case SERVICES_REPLY -> ofServicesReply(method);
            case UNKNOWN -> UNKNOWN;
        };
    }

    private static MqttScenario ofRequest(String method) {
        if (method == null) {
            return UNKNOWN;
        }
        return switch (method) {
            case "config" -> LICENSE_CONFIG;
            case "airport_bind_status" -> BIND_STATUS;
            case "airport_organization_get" -> ORG_GET;
            case "airport_organization_bind" -> ORG_BIND;
            case "storage_config_get" -> STORAGE_CONFIG;
            case "flighttask_resource_get" -> WAYLINE_RESOURCE_GET;
            default -> UNKNOWN;
        };
    }

    private static MqttScenario ofEvent(String method) {
        if (method == null) {
            return UNKNOWN;
        }
        if ("hms".equals(method)) {
            return ALARM_HMS;
        }
        if ("ota_progress".equals(method)) {
            return FIRMWARE_PROGRESS;
        }
        if ("fileupload_progress".equals(method)) {
            return REMOTE_LOG_PROGRESS;
        }
        if ("airsense_warning".equals(method)) {
            return AIRSENSE_WARNING;
        }
        if ("file_upload_callback".equals(method)) {
            return MEDIA_UPLOAD_CALLBACK;
        }
        if ("highest_priority_upload_flighttask_media".equals(method)) {
            return MEDIA_PRIORITY_QUERY;
        }
        if ("flight_areas_sync_progress".equals(method)) {
            return FLIGHT_AREAS_SYNC_PROGRESS;
        }
        if ("flight_areas_drone_location".equals(method)) {
            return FLIGHT_AREAS_WARNING;
        }
        if ("flighttask_progress".equals(method)) {
            return WAYLINE_PROGRESS;
        }
        if ("fly_to_point_progress".equals(method)) {
            return FLY_TO_PROGRESS;
        }
        if ("takeoff_to_point_progress".equals(method)) {
            return TAKEOFF_PROGRESS;
        }
        if ("drc_status_notify".equals(method)) {
            return DRC_STATUS;
        }
        return UNKNOWN;
    }

    private static MqttScenario ofServicesReply(String method) {
        if (method == null) {
            return UNKNOWN;
        }
        if ("ota_create".equals(method)) {
            return FIRMWARE_REPLY;
        }
        if (method.startsWith("fileupload_")) {
            return REMOTE_LOG_REPLY;
        }
        if (method.startsWith("live_")) {
            return LIVE_REPLY;
        }
        if ("flight_areas_update".equals(method)) {
            return FLIGHT_AREAS_REPLY;
        }
        if (method.startsWith("flighttask_")) {
            return WAYLINE_REPLY;
        }
        return UNKNOWN;
    }
}
