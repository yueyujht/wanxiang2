package cn.wanxing.device.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 拓扑消息的 data 部分（设备上下线时的上报内容）
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopologyData {

    /** 设备域 */
    private Integer domain;

    /** 设备型号 */
    private Integer type;

    /** 设备子型号 */
    private Integer subType;

    /** 设备密钥 */
    private String deviceSecret;

    private String nonce;

    private String thingVersion;

    /** 子设备列表：非空表示上线，空/null 表示离线 */
    private List<SubDeviceInfo> subDevices;
}