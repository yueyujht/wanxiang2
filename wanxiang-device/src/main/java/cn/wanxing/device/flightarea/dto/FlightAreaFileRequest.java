package cn.wanxing.device.flightarea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 自定义飞行区文件创建请求
 */
@Getter
@Setter
public class FlightAreaFileRequest {

    /** 文件名（如 geofence_park_a.json） */
    @NotBlank(message = "文件名不能为空")
    @Size(max = 128, message = "文件名最长 128 字符")
    private String name;

    /** 文件内容（官方自定义飞行区 JSON 格式） */
    @NotBlank(message = "文件内容不能为空")
    @Size(max = 2_000_000, message = "文件内容不能超过 2MB")
    private String content;
}
