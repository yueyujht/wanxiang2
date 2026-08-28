package cn.wanxing.device.status.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备属性设置请求
 */
@Getter
@Setter
public class DevicePropertySetRequest {

    /** 属性名，如 silent_mode / air_transfer_enable / user_experience_improvement */
    @NotBlank(message = "属性名不能为空")
    private String property;

    /** 属性值（可为标量、布尔或对象，如 {"state": 1}） */
    @NotNull(message = "属性值不能为空")
    private JsonNode value;
}
