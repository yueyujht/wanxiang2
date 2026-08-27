package cn.wanxing.device.exception;

import cn.wanxing.common.exception.ErrorCode;

/**
 * 设备模块错误码
 */
public enum DeviceErrorCode implements ErrorCode {

    DEVICE_ALREADY_BOUND("DEVICE_ALREADY_BOUND", "设备已绑定"),
    DEVICE_NOT_FOUND("DEVICE_NOT_FOUND", "设备不存在"),
    ORG_NOT_FOUND("ORG_NOT_FOUND", "机构不存在"),
    ORG_DISABLED("ORG_DISABLED", "机构已被停用"),
    OPERATION_FORBIDDEN("OPERATION_FORBIDDEN", "无权执行该操作"),
    INSERT_FAILED("INSERT_FAILED", "新增设备失败"),
    UPDATE_FAILED("UPDATE_FAILED", "更新设备失败"),
    PROPERTY_SET_FAILED("PROPERTY_SET_FAILED", "属性设置下发失败"),
    PROPERTY_VALUE_INVALID("PROPERTY_VALUE_INVALID", "属性值不合法"),
    FIRMWARE_UPGRADE_FAILED("FIRMWARE_UPGRADE_FAILED", "固件升级下发失败");

    private final String code;

    private final String message;

    DeviceErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}