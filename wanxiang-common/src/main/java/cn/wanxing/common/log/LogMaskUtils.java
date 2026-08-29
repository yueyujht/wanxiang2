package cn.wanxing.common.log;

/**
 * 日志脱敏工具：手机号等敏感信息落日志前先处理
 */
public final class LogMaskUtils {

    private LogMaskUtils() {
    }

    /**
     * 手机号脱敏：138****0000。为空或位数不足（可能已是非标准号码）时整体打码
     */
    public static String maskMobile(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
