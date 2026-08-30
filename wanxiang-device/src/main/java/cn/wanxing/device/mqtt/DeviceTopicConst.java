package cn.wanxing.device.mqtt;

/**
 * DJI 上云 API 的 MQTT 主题常量。
 *
 * <p>两类前缀：
 * <ul>
 *   <li>{@code sys/product/{sn}/status} —— 设备上下线</li>
 *   <li>{@code thing/product/{sn}/...} —— 业务数据（OSD/状态/事件/控制）</li>
 * </ul>
 */
public final class DeviceTopicConst {

    private DeviceTopicConst() {
    }

    /** 前缀 */
    public static final String SYS_PRE = "sys/";
    public static final String THING_PRE = "thing/";
    public static final String PRODUCT = "product/";

    /** 后缀 */
    public static final String STATUS_SUF = "/status";
    public static final String OSD_SUF = "/osd";
    public static final String STATE_SUF = "/state";
    public static final String EVENTS_SUF = "/events";
    public static final String SERVICES_SUF = "/services";
    public static final String SERVICES_REPLY_SUF = "/services_reply";
    public static final String REQUESTS_SUF = "/requests";
    public static final String REQUESTS_REPLY_SUF = "/requests_reply";
    public static final String PROPERTY_SET_SUF = "/property/set";
    public static final String PROPERTY_SET_REPLY_SUF = "/property/set_reply";
    /** DRC 高频通道（摇杆/HSI/高频 OSD），down 云到设备 / up 设备到云 */
    public static final String DRC_DOWN_SUF = "/drc/down";
    public static final String DRC_UP_SUF = "/drc/up";

    /** 回复主题后缀 */
    public static final String REPLY_SUF = "_reply";
}