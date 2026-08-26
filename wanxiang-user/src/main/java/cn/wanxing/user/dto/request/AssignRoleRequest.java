package cn.wanxing.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 分配角色请求
 */
@Getter
@Setter
public class AssignRoleRequest {

    @NotBlank(message = "角色不能为空")
    private String role;
}
