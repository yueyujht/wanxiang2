package cn.wanxing.common.constant;

/**
 * 权限点常量（模块:动作）
 */
public final class PermissionConst {

    private PermissionConst() {
    }

    // 用户
    public static final String USER_READ = "user:read";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DISABLE = "user:disable";

    // 角色
    public static final String ROLE_READ = "role:read";
    public static final String ROLE_MANAGE = "role:manage";

    // 机构（租户）
    public static final String ORG_READ = "org:read";
    public static final String ORG_CREATE = "org:create";
    public static final String ORG_UPDATE = "org:update";

    // 项目
    public static final String PROJECT_READ = "project:read";
    public static final String PROJECT_CREATE = "project:create";
    public static final String PROJECT_UPDATE = "project:update";
    public static final String PROJECT_DELETE = "project:delete";

    // 设备
    public static final String DEVICE_READ = "device:read";
    public static final String DEVICE_BIND = "device:bind";
    public static final String DEVICE_CONFIG = "device:config";
    public static final String DEVICE_CONTROL = "device:control";

    // 航线
    public static final String ROUTE_READ = "route:read";
    public static final String ROUTE_CREATE = "route:create";
    public static final String ROUTE_UPDATE = "route:update";
    public static final String ROUTE_DELETE = "route:delete";

    // 任务
    public static final String TASK_READ = "task:read";
    public static final String TASK_CREATE = "task:create";
    public static final String TASK_EXECUTE = "task:execute";
    public static final String TASK_CONTROL = "task:control";

    // 直播
    public static final String LIVE_READ = "live:read";
    public static final String LIVE_CONTROL = "live:control";

    // 媒体
    public static final String MEDIA_READ = "media:read";
    public static final String MEDIA_DOWNLOAD = "media:download";
    public static final String MEDIA_DELETE = "media:delete";

    // 告警
    public static final String ALARM_READ = "alarm:read";
    public static final String ALARM_HANDLE = "alarm:handle";

    // 固件
    public static final String FIRMWARE_READ = "firmware:read";
    public static final String FIRMWARE_UPGRADE = "firmware:upgrade";

    // 系统
    public static final String SYSTEM_CONFIG = "system:config";
    public static final String SYSTEM_AUDIT = "system:audit";
    public static final String SYSTEM_MONITOR = "system:monitor";
}