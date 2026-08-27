package cn.wanxing.device.firmware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 固件升级请求
 */
@Getter
@Setter
public class FirmwareUpgradeRequest {

    /** 固件文件下载地址 */
    @NotBlank(message = "固件下载地址不能为空")
    private String fileUrl;

    /** 固件文件名称 */
    @NotBlank(message = "固件文件名不能为空")
    private String fileName;

    /** 固件文件 MD5 */
    @NotBlank(message = "固件 MD5 不能为空")
    private String md5;

    /** 固件文件大小（字节） */
    @NotNull(message = "固件大小不能为空")
    private Long fileSize;

    /** 目标固件版本 */
    @NotBlank(message = "目标版本不能为空")
    private String targetVersion;

    /** 升级类型：2 一致性升级 / 3 普通升级 / 4 PSDK升级 */
    @NotNull(message = "升级类型不能为空")
    private Integer upgradeType;
}