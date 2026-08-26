package cn.wanxing.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建/更新机构请求
 */
@Getter
@Setter
public class CreateOrgRequest {

    @NotBlank(message = "机构名称不能为空")
    @Size(max = 64, message = "机构名称长度不能超过 64")
    private String name;

    @NotBlank(message = "机构编码不能为空")
    @Size(max = 64, message = "机构编码长度不能超过 64")
    private String code;

    @Size(max = 256, message = "描述长度不能超过 256")
    private String description;

    /** 状态（仅更新时使用，可选） */
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态非法")
    private String status;
}
