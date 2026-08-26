package cn.wanxing.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台建账号（录入手机号）请求
 */
@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "角色不能为空")
    private String role;

    @Size(max = 32, message = "昵称长度不能超过 32")
    private String nickname;

    /** 所属机构：平台超管建号时必填；机构管理员建号时忽略（默认本机构） */
    private Long orgId;
}
