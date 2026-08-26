package cn.wanxing.user.entity;

import cn.wanxing.user.constant.UserStateEnum;
import cn.wanxing.user.dto.request.CreateUserRequest;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体（sys_user）
 */
@Setter
@Getter
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构 ID（平台超管为 null） */
    private Long orgId;

    /** 手机号，唯一（登录标识） */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 角色编码（单角色，ADMIN/OPERATOR/OBSERVER/SUPER_ADMIN 或自定义） */
    private String role;

    /** 状态 */
    private UserStateEnum status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建用户
     */
    public static User createUser(CreateUserRequest req, Long orgId) {
        User user = new User();
        user.orgId = orgId;
        user.phone = req.getPhone();
        user.nickname = req.getNickname();
        user.role = req.getRole();
        user.status = UserStateEnum.ENABLED;
        return user;
    }
}
