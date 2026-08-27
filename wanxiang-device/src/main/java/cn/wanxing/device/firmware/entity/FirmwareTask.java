package cn.wanxing.device.firmware.entity;

import cn.wanxing.device.firmware.dto.FirmwareUpgradeRequest;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 固件升级任务实体（sys_firmware_task）：记录一次升级请求及最终结果。
 */
@Getter
@Setter
@TableName("sys_firmware_task")
public class FirmwareTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备 SN */
    private String deviceSn;

    /** 目标固件版本 */
    private String targetVersion;

    /** 固件文件名 */
    private String fileName;

    /** 固件下载地址 */
    private String fileUrl;

    /** 固件 MD5 */
    private String md5;

    /** 固件大小（字节） */
    private Long fileSize;

    /** 升级类型：2 一致性 / 3 普通 / 4 PSDK */
    private Integer upgradeType;

    /** 任务状态：sent / rejected / in_progress / ok / failed / canceled / timeout */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 创建固件升级记录
    public static FirmwareTask create(String sn, FirmwareUpgradeRequest req) {
        FirmwareTask task = new FirmwareTask();
        task.setDeviceSn(sn);
        task.setTargetVersion(req.getTargetVersion());
        task.setFileName(req.getFileName());
        task.setFileUrl(req.getFileUrl());
        task.setMd5(req.getMd5());
        task.setFileSize(req.getFileSize());
        task.setUpgradeType(req.getUpgradeType());
        task.setStatus("sent");
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    // 更新固件升级状态
    public static void updateState(FirmwareTask task, String status) {
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
    }

}