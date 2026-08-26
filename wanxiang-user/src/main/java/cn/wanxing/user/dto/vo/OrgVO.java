package cn.wanxing.user.dto.vo;

import cn.wanxing.user.entity.Org;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 机构（租户）信息 VO
 */
@Getter
@Setter
public class OrgVO {

    private Long id;

    private String name;

    private String code;

    /** 设备绑定码 */
    private String bindCode;

    private String description;

    /** ENABLED / DISABLED */
    private String status;

    private LocalDateTime createdAt;

    public static OrgVO from(Org org) {
        OrgVO vo = new OrgVO();
        vo.id = org.getId();
        vo.name = org.getName();
        vo.code = org.getCode();
        vo.bindCode = org.getBindCode();
        vo.description = org.getDescription();
        vo.status = org.getStatus() != null ? org.getStatus().name() : null;
        vo.createdAt = org.getCreatedAt();
        return vo;
    }
}
