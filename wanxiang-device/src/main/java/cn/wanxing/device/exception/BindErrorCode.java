package cn.wanxing.device.exception;

/**
 * DJI 上云 API 业务错误码（用于 MQTT 请求-应答中的 result / err_code 字段）。
 *
 * <p>注意：这是 DJI 协议规定的标准错误码，不是我们内部的业务错误码。
 */
public final class BindErrorCode {

    private BindErrorCode() {
    }

    /** 获取组织失败 */
    public static final int GET_ORGANIZATION_FAILED = 210230;

    /** 设备绑定失败 */
    public static final int DEVICE_BINDING_FAILED = 210231;

    /** 设备已绑定到其它组织，不可重复绑定 */
    public static final int NON_REPEATABLE_BINDING = 210232;
}
