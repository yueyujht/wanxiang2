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
    FIRMWARE_UPGRADE_FAILED("FIRMWARE_UPGRADE_FAILED", "固件升级下发失败"),
    LIVE_URL_MISSING("LIVE_URL_MISSING", "直播推流地址未配置"),
    LIVE_COMMAND_FAILED("LIVE_COMMAND_FAILED", "直播指令下发失败"),
    MEDIA_FILE_NOT_FOUND("MEDIA_FILE_NOT_FOUND", "媒体文件不存在"),
    FLIGHT_AREA_FILE_NOT_FOUND("FLIGHT_AREA_FILE_NOT_FOUND", "飞行区文件不存在"),
    FLIGHT_AREA_SYNC_FAILED("FLIGHT_AREA_SYNC_FAILED", "飞行区同步指令下发失败"),
    WAYLINE_FILE_NOT_FOUND("WAYLINE_FILE_NOT_FOUND", "航线文件不存在"),
    WAYLINE_JOB_NOT_FOUND("WAYLINE_JOB_NOT_FOUND", "航线任务不存在"),
    WAYLINE_JOB_STATE_INVALID("WAYLINE_JOB_STATE_INVALID", "任务当前状态不允许该操作"),
    WAYLINE_COMMAND_FAILED("WAYLINE_COMMAND_FAILED", "航线任务指令下发失败"),
    DRC_NOT_CONFIGURED("DRC_NOT_CONFIGURED", "DRC 通道凭证未配置"),
    DRC_COMMAND_FAILED("DRC_COMMAND_FAILED", "DRC 指令下发失败");

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