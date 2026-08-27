package cn.wanxing.device.bind;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 设备发来的请求信封（thing/product/{sn}/requests 主题的报文）。
 *
 * <p>data 字段保留为原始 JSON 树，由各 method 按需解析。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestsMessage {

    /** 事务 ID（回复时必须原样回传） */
    private String tid;

    /** 业务 ID（回复时必须原样回传） */
    private String bid;

    /** 时间戳 */
    private Long timestamp;

    /** 方法名：config / airport_bind_status / airport_organization_get / airport_organization_bind */
    private String method;

    /** 网关设备 SN */
    private String gateway;

    /** 方法对应的数据 */
    private JsonNode data;
}
