package cn.wanxing.device.wayline.entity;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 航线文件实体（sys_wayline_file）：内容为 DJI WPML 标准的 KMZ 文件（zip 容器），
 * 平台存储，设备经 flighttask_resource_get 获取下载地址后拉取
 */
@Getter
@Setter
@TableName("sys_wayline_file")
public class WaylineFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构 ID（NULL=全局，全部机构可见） */
    private Long orgId;

    /** 航线文件名（如 xxx.kmz） */
    private String name;

    /** KMZ 文件内容 */
    private byte[] content;

    /** 文件 MD5 签名（协议 fingerprint，设备校验用） */
    private String fingerprint;

    /** 文件大小（字节） */
    private Long size;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 由名称与文件字节构建实体：MD5 fingerprint 与大小在此处计算
     */
    public static WaylineFile create(String name, byte[] content) {
        WaylineFile file = new WaylineFile();
        file.setName(name);
        file.setContent(content);
        // 协议 file.fingerprint 明确为 MD5 签名
        file.setFingerprint(DigestUtil.md5Hex(content));
        file.setSize((long) content.length);
        return file;
    }
}
