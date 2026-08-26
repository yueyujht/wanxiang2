package cn.wanxing.device.exception;

import cn.wanxing.common.exception.BizException;
import cn.wanxing.common.exception.ErrorCode;

/**
 * 设备模块业务异常
 */
public class DeviceException extends BizException {

    public DeviceException(ErrorCode errorCode) {
        super(errorCode);
    }
}