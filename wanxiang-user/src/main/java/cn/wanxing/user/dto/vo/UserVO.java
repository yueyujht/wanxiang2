package cn.wanxing.user.dto.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户信息（脱敏，不含敏感字段）
 */
@Setter
@Getter
@Builder
public class UserVO {

    private Long id;

    /** 手机号 */
    private String phone;

    private String nickname;

    private String avatar;

    /** 角色编码（SUPER_ADMIN/ADMIN/OPERATOR/OBSERVER 或自定义） */
    private String role;

    /** 角色名 */
    private String roleName;

    /** 所属机构 ID（平台超管为 null） */
    private Long orgId;

    /** 机构名称 */
    private String orgName;

    /** 状态：ENABLED / DISABLED */
    private String status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
}
