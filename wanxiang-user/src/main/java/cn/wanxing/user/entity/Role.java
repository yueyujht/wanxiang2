package cn.wanxing.user.entity;

import cn.wanxing.user.constant.EnableStatusEnum;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 角色实体（sys_role）
 */
@Setter
@Getter
@TableName("sys_role")
public class Role {

    /** 主键（内部自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构 ID（NULL = 全局预定义角色） */
    private Long orgId;

    /** 角色编码（机构内唯一，可读，如 ADMIN） */
    private String code;

    /** 角色名 */
    private String name;

    /** 描述 */
    private String description;

    /** 1 预定义 / 0 自定义 */
    private Integer isBuiltin;

    /** 启用 / 停用 */
    private EnableStatusEnum status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
