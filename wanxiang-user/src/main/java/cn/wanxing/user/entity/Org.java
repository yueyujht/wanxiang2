package cn.wanxing.user.entity;

import cn.wanxing.user.constant.EnableStatusEnum;
import cn.wanxing.user.dto.request.CreateOrgRequest;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 机构（租户）实体（sys_org）
 */
@Setter
@Getter
@TableName("sys_org")
public class Org {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机构名称 */
    private String name;

    /** 机构编码，唯一 */
    private String code;

    /** 设备绑定码（现场操作员在 Pilot 中填写），唯一 */
    private String bindCode;

    /** 描述 */
    private String description;

    /** 启用 / 停用 */
    private EnableStatusEnum status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建机构
     */
    public static Org createOrg(CreateOrgRequest req) {
        Org org = new Org();
        org.name = req.getName();
        org.code = req.getCode();
        org.bindCode = generateBindCode();
        org.description = req.getDescription();
        org.status = EnableStatusEnum.ENABLED;
        return org;
    }

    /**
     * 生成设备绑定码（8 位大写，唯一性由数据库唯一索引兜底）
     */
    private static String generateBindCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 更新机构
     */
    public static Org updateOrg(Org org, CreateOrgRequest req) {
        org.setName(req.getName());
        org.setCode(req.getCode());
        org.setDescription(req.getDescription());
        if (req.getStatus() != null) {
            org.setStatus(EnableStatusEnum.valueOf(req.getStatus()));
        }
        return org;
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return this.status == EnableStatusEnum.ENABLED;
    }
}
