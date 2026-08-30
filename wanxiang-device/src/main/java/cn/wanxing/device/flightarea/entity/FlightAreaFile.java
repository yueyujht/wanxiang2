package cn.wanxing.device.flightarea.entity;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 自定义飞行区文件实体（sys_flight_area_file）：内容为官方 JSON 模板格式（作业区/限飞区多边形），
 * 平台生成并存储，设备经 flight_areas_get 获取列表后从平台 HTTP 接口下载
 */
@Getter
@Setter
@TableName("sys_flight_area_file")
public class FlightAreaFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构 ID（NULL=全局，全部机构可见） */
    private Long orgId;

    /** 文件名（如 geofence_park_a.json） */
    private String name;

    /** 文件内容（官方自定义飞行区 JSON 格式） */
    private String content;

    /** 文件 SHA256 签名（设备用于版本判断，不一致则以云端版本为准） */
    private String checksum;

    /** 文件大小（字节） */
    private Integer size;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 由名称与内容构建文件实体：SHA256 签名与大小在此处计算（按 UTF-8 字节）
     */
    public static FlightAreaFile create(String name, String content) {
        FlightAreaFile file = new FlightAreaFile();
        file.setName(name);
        file.setContent(content);
        file.setChecksum(DigestUtil.sha256Hex(content.getBytes(StandardCharsets.UTF_8)));
        file.setSize(content.getBytes(StandardCharsets.UTF_8).length);
        return file;
    }
}
