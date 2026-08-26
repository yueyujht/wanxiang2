-- ===================== 用户系统表（多租户） =====================

CREATE TABLE IF NOT EXISTS sys_org (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '机构名称',
    code        VARCHAR(64)  NOT NULL COMMENT '机构编码',
    bind_code   VARCHAR(64)  DEFAULT NULL COMMENT '设备绑定码（现场操作员在 Pilot 中填写）',
    description VARCHAR(256) DEFAULT NULL COMMENT '描述',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED 启用 / DISABLED 停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_name (name),
    UNIQUE KEY uk_org_code (code),
    UNIQUE KEY uk_org_bind_code (bind_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '机构（租户）表';

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    org_id      BIGINT       DEFAULT NULL COMMENT '所属机构 ID（NULL=全局预定义角色）',
    code        VARCHAR(64)  NOT NULL COMMENT '角色编码（机构内唯一，可读，如 ADMIN）',
    name        VARCHAR(64)  NOT NULL COMMENT '角色名',
    description VARCHAR(256) DEFAULT NULL COMMENT '描述',
    is_builtin  TINYINT      NOT NULL DEFAULT 1 COMMENT '1 预定义 0 自定义',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED 启用 / DISABLED 停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_role_org_code (org_id, code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id    BIGINT      NOT NULL COMMENT '角色 ID',
    permission VARCHAR(64) NOT NULL COMMENT '权限点，如 device:control',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限表';

CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    org_id        BIGINT       DEFAULT NULL COMMENT '所属机构 ID（平台超管为 NULL）',
    phone         VARCHAR(20)  NOT NULL COMMENT '手机号（登录标识）',
    nickname      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar        VARCHAR(256) DEFAULT NULL COMMENT '头像 URL',
    role          VARCHAR(64)  NOT NULL COMMENT '角色编码（机构内唯一，全局角色对所有机构可见）',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED 启用 / DISABLED 禁用',
    last_login_at DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone),
    KEY idx_org (org_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    org_id     BIGINT       DEFAULT NULL COMMENT '所属机构 ID',
    user_id    BIGINT       DEFAULT NULL COMMENT '用户 ID',
    action     VARCHAR(64)  DEFAULT NULL COMMENT '动作',
    resource   VARCHAR(128) DEFAULT NULL COMMENT '资源',
    detail     TEXT         COMMENT '详情',
    ip         VARCHAR(64)  DEFAULT NULL COMMENT 'IP',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审计日志表';

CREATE TABLE IF NOT EXISTS sys_device (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    sn             VARCHAR(64)  NOT NULL COMMENT '设备序列号',
    name           VARCHAR(64)  DEFAULT NULL COMMENT '设备名称（绑定时填写的 device_callsign）',
    org_id         BIGINT       DEFAULT NULL COMMENT '所属机构 ID（未绑定为 NULL）',
    parent_sn      VARCHAR(64)  DEFAULT NULL COMMENT '父设备 SN（如无人机所属机场）',
    domain         INT          DEFAULT NULL COMMENT '设备域 0 无人机 / 1 负载 / 2 遥控器 / 3 机场',
    type           INT          DEFAULT NULL COMMENT '设备型号',
    sub_type       INT          DEFAULT NULL COMMENT '设备子型号',
    model_name       VARCHAR(64)  DEFAULT NULL COMMENT '设备型号名称（如 DJI Dock / Matrice 30）',
    firmware_version VARCHAR(32)  DEFAULT NULL COMMENT '固件版本（来自 state 消息）',
    device_index     VARCHAR(32)  DEFAULT NULL COMMENT '设备索引（遥控器 A控/B控）',
    status           VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE 在线 / OFFLINE 离线',
    last_online_at   DATETIME     DEFAULT NULL COMMENT '最近上线时间',
    bound_at         DATETIME     DEFAULT NULL COMMENT '绑定组织时间（解绑清空）',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sn (sn),
    KEY idx_org (org_id),
    KEY idx_parent_sn (parent_sn)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备表';

-- ===================== 种子数据 =====================

-- 预定义角色（全局，org_id 为 NULL；id 为内部自增主键，业务上以 code 标识）
INSERT IGNORE INTO sys_role (id, org_id, code, name, description, is_builtin, status) VALUES
    (1, NULL, 'ADMIN', '管理员', '机构管理员：管理本机构用户与业务', 1, 'ENABLED'),
    (2, NULL, 'OPERATOR', '操作员', '日常作业：设备/任务/直播/媒体/告警', 1, 'ENABLED'),
    (3, NULL, 'OBSERVER', '观察员', '只读：查看设备/任务/媒体等', 1, 'ENABLED'),
    (4, NULL, 'SUPER_ADMIN', '平台超管', '平台超级管理员：跨机构管理', 1, 'ENABLED');

-- 演示机构
INSERT IGNORE INTO sys_org (id, name, code, bind_code, description, status) VALUES
    (1, '演示机构', 'demo', 'DEMO001', '默认演示机构', 'ENABLED'),
    (2, '某园区', 'park_a', 'PARK002', '甲方 A 园区', 'ENABLED');

-- 默认账号（手机号登录，验证码 mock 固定 123456）
-- 平台超管：org_id 为 NULL
INSERT IGNORE INTO sys_user (id, org_id, phone, nickname, role, status) VALUES
    (1, NULL, '13800000000', '平台管理员', 'SUPER_ADMIN', 'ENABLED'),
    (2, 1, '13900000001', '机构管理员', 'ADMIN', 'ENABLED'),
    (3, 1, '13900000002', '机构操作员', 'OPERATOR', 'ENABLED');

-- 测试设备（已绑定到机构）
INSERT IGNORE INTO sys_device (id, sn, name, org_id, parent_sn, domain, type, sub_type, model_name, status) VALUES
    (1, 'DOCK001', '一号机场', 1, NULL, 3, 1, 0, 'DJI Dock', 'OFFLINE'),
    (2, 'DRONE001', '一号机', 1, 'DOCK001', 0, 67, 0, 'Matrice 30', 'OFFLINE'),
    (3, 'DOCK002', '园区机场', 2, NULL, 3, 1, 0, 'DJI Dock', 'OFFLINE');

-- 平台超管权限（全部，role_id=4）
INSERT IGNORE INTO sys_role_permission (role_id, permission) VALUES
    (4, 'user:read'), (4, 'user:create'), (4, 'user:update'), (4, 'user:disable'),
    (4, 'role:read'), (4, 'role:manage'),
    (4, 'org:read'), (4, 'org:create'), (4, 'org:update'),
    (4, 'project:read'), (4, 'project:create'), (4, 'project:update'), (4, 'project:delete'),
    (4, 'device:read'), (4, 'device:bind'), (4, 'device:config'), (4, 'device:control'),
    (4, 'route:read'), (4, 'route:create'), (4, 'route:update'), (4, 'route:delete'),
    (4, 'task:read'), (4, 'task:create'), (4, 'task:execute'), (4, 'task:control'),
    (4, 'live:read'), (4, 'live:control'),
    (4, 'media:read'), (4, 'media:download'), (4, 'media:delete'),
    (4, 'alarm:read'), (4, 'alarm:handle'),
    (4, 'firmware:read'), (4, 'firmware:upgrade'),
    (4, 'system:config'), (4, 'system:audit'), (4, 'system:monitor');

-- 机构管理员权限（本机构用户 + 业务，不含机构/系统管理，role_id=1）
INSERT IGNORE INTO sys_role_permission (role_id, permission) VALUES
    (1, 'user:read'), (1, 'user:create'), (1, 'user:update'), (1, 'user:disable'),
    (1, 'role:read'),
    (1, 'project:read'), (1, 'project:create'), (1, 'project:update'), (1, 'project:delete'),
    (1, 'device:read'), (1, 'device:bind'), (1, 'device:config'), (1, 'device:control'),
    (1, 'route:read'), (1, 'route:create'), (1, 'route:update'), (1, 'route:delete'),
    (1, 'task:read'), (1, 'task:create'), (1, 'task:execute'), (1, 'task:control'),
    (1, 'live:read'), (1, 'live:control'),
    (1, 'media:read'), (1, 'media:download'), (1, 'media:delete'),
    (1, 'alarm:read'), (1, 'alarm:handle'),
    (1, 'firmware:read'), (1, 'firmware:upgrade');

-- 操作员权限（role_id=2）
INSERT IGNORE INTO sys_role_permission (role_id, permission) VALUES
    (2, 'device:read'), (2, 'device:bind'), (2, 'device:config'), (2, 'device:control'),
    (2, 'route:read'), (2, 'route:create'), (2, 'route:update'), (2, 'route:delete'),
    (2, 'task:read'), (2, 'task:create'), (2, 'task:execute'), (2, 'task:control'),
    (2, 'live:read'), (2, 'live:control'),
    (2, 'media:read'), (2, 'media:download'), (2, 'media:delete'),
    (2, 'alarm:read'), (2, 'alarm:handle'),
    (2, 'project:read'), (2, 'firmware:read');

-- 观察员权限（role_id=3）
INSERT IGNORE INTO sys_role_permission (role_id, permission) VALUES
    (3, 'device:read'), (3, 'task:read'), (3, 'route:read'), (3, 'live:read'),
    (3, 'media:read'), (3, 'alarm:read'), (3, 'project:read');
