package cn.wanxing.device.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备重命名请求
 */
@Getter
@Setter
public class DeviceRenameRequest {

    /** 设备名称（绑定时填写的 device_callsign） */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 64, message = "设备名称不能超过 64 个字符")
    private String name;
}