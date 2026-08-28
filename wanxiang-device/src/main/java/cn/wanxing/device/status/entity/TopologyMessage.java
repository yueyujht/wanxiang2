package cn.wanxing.device.status.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 设备上下线消息信封（sys/product/{sn}/status 主题的报文）
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopologyMessage {

    /** 事务 ID */
    private String tid;

    /** 业务 ID */
    private String bid;

    /** 时间戳 */
    private Long timestamp;

    /** 方法名（update_topo 等） */
    private String method;

    private TopologyData data;
}