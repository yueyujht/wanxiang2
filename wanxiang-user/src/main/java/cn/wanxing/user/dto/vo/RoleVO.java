package cn.wanxing.user.dto.vo;

import cn.wanxing.user.entity.Role;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色信息 VO
 */
@Getter
@Setter
public class RoleVO {

    /** 角色 ID */
    private Long id;

    /** 角色编码 */
    private String code;

    /** 所属机构 ID（NULL = 全局预定义角色） */
    private Long orgId;

    private String name;

    private String description;

    /** 1 预定义 / 0 自定义 */
    private Integer isBuiltin;

    /** ENABLED / DISABLED */
    private String status;

    public static RoleVO from(Role role) {
        RoleVO vo = new RoleVO();
        vo.id = role.getId();
        vo.code = role.getCode();
        vo.orgId = role.getOrgId();
        vo.name = role.getName();
        vo.description = role.getDescription();
        vo.isBuiltin = role.getIsBuiltin();
        vo.status = role.getStatus() != null ? role.getStatus().name() : null;
        return vo;
    }
}
