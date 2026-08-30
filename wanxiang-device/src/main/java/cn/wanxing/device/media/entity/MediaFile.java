package cn.wanxing.device.media.entity;

import cn.wanxing.device.media.message.MediaFileInfo;
import cn.wanxing.device.media.message.MediaTaskInfo;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 媒体文件实体（sys_media_file）：设备直传对象存储后的文件元数据，本表只做索引不存文件本体
 */
@Getter
@Setter
@TableName("sys_media_file")
public class MediaFile {

    /** 官方示例的拍摄时间格式（个别固件为 ISO8601，解析失败兜底） */
    private static final DateTimeFormatter SHOOT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上报的网关设备 SN（机场） */
    private String deviceSn;

    /** 文件在对象存储桶的 Key（唯一） */
    private String objectKey;

    /** 文件名称 */
    private String fileName;

    /** 文件的业务路径 */
    private String filePath;

    /** 所属任务 ID */
    private String flightId;

    /** 飞行器型号枚举（domain-type-subtype） */
    private String droneModelKey;

    /** 负载型号枚举（domain-type-subtype） */
    private String payloadModelKey;

    /** 是否原图 */
    private Boolean isOriginal;

    /** 云台偏航角（度） */
    private Double gimbalYaw;

    /** 拍摄绝对高度（米） */
    private Double absoluteAltitude;

    /** 拍摄相对高度（米） */
    private Double relativeAltitude;

    /** 拍摄位置纬度 */
    private Double shootLat;

    /** 拍摄位置经度 */
    private Double shootLng;

    /** 媒体拍摄时间 */
    private LocalDateTime shootTime;

    /** 该飞行架次当前已上传媒体数量 */
    private Integer uploadedCount;

    /** 该飞行架次拍摄媒体总数量 */
    private Integer expectedCount;

    /** 飞行类型：0 航线任务 / 1 一键起飞任务 */
    private Integer flightType;

    /** 入库时间 */
    private LocalDateTime createdAt;

    /**
     * 由一条上传结果构建媒体文件实体
     */
    public static MediaFile create(String sn, MediaFileInfo file, MediaTaskInfo task) {
        MediaFile media = new MediaFile();
        media.setDeviceSn(sn);
        media.setObjectKey(file.getObjectKey());
        media.setFileName(file.getName());
        media.setFilePath(file.getPath());
        MediaFileInfo.Ext ext = file.getExt();
        if (ext != null) {
            media.setFlightId(ext.getFlightId());
            media.setDroneModelKey(ext.getDroneModelKey());
            media.setPayloadModelKey(ext.getPayloadModelKey());
            media.setIsOriginal(ext.getIsOriginal());
        }
        MediaFileInfo.Metadata metadata = file.getMetadata();
        if (metadata != null) {
            media.setGimbalYaw(metadata.getGimbalYawDegree());
            media.setAbsoluteAltitude(metadata.getAbsoluteAltitude());
            media.setRelativeAltitude(metadata.getRelativeAltitude());
            media.setShootTime(parseShootTime(metadata.getCreateTime()));
            MediaFileInfo.ShootPosition position = metadata.getShootPosition();
            if (position != null) {
                media.setShootLat(position.getLat());
                media.setShootLng(position.getLng());
            }
        }
        if (task != null) {
            media.setUploadedCount(task.getUploadedFileCount());
            media.setExpectedCount(task.getExpectedFileCount());
            media.setFlightType(task.getFlightType());
        }
        media.setCreatedAt(LocalDateTime.now());
        return media;
    }

    /**
     * 拍摄时间解析：优先官方示例格式，兜底 ISO8601，再失败返回 null（不因时间格式丢文件记录）
     */
    private static LocalDateTime parseShootTime(String createTime) {
        if (createTime == null || createTime.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(createTime, SHOOT_TIME_FORMAT);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(createTime);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
