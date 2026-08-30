package cn.wanxing.device.mqtt;

/**
 * 消息主题类型：用于把收到的一串主题字符串归成几类，再派发给对应处理器。
 */
public enum DeviceTopicType {

    /** 上下线：sys/product/{sn}/status */
    ONLINE_OFFLINE,

    /** 实时飞行数据：thing/product/{sn}/osd */
    OSD,

    /** 状态变更：thing/product/{sn}/state */
    STATE,

    /** 事件/告警：thing/product/{sn}/events */
    EVENTS,

    /** 控制指令应答：thing/product/{sn}/services_reply */
    SERVICES_REPLY,

    /** 设备发来的请求：thing/product/{sn}/requests（如绑定、License 校验、资源获取） */
    REQUESTS,

    /** 属性设置回执：thing/product/{sn}/property/set_reply */
    PROPERTY_SET_REPLY,

    /** DRC 高频通道上行：thing/product/{sn}/drc/up（摇杆/HSI/高频 OSD，平台透传给前端） */
    DRC_UP,

    /** 未知类型（暂不处理） */
    UNKNOWN;

    /**
     * 按主题后缀识别类型
     */
    public static DeviceTopicType fromTopic(String topic) {
        if (topic == null) {
            return UNKNOWN;
        }
        if (topic.endsWith(DeviceTopicConst.STATUS_SUF)) {
            return ONLINE_OFFLINE;
        }
        // drc/up 需在 osd/state 之前判断（无后缀冲突，仅求语义清晰）
        if (topic.endsWith(DeviceTopicConst.DRC_UP_SUF)) {
            return DRC_UP;
        }
        if (topic.endsWith(DeviceTopicConst.OSD_SUF)) {
            return OSD;
        }
        if (topic.endsWith(DeviceTopicConst.STATE_SUF)) {
            return STATE;
        }
        if (topic.endsWith(DeviceTopicConst.EVENTS_SUF)) {
            return EVENTS;
        }
        if (topic.endsWith(DeviceTopicConst.SERVICES_REPLY_SUF)) {
            return SERVICES_REPLY;
        }
        if (topic.endsWith(DeviceTopicConst.REQUESTS_SUF)) {
            return REQUESTS;
        }
        if (topic.endsWith(DeviceTopicConst.PROPERTY_SET_REPLY_SUF)) {
            return PROPERTY_SET_REPLY;
        }
        return UNKNOWN;
    }
}
