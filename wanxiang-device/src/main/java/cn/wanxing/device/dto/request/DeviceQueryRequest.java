package cn.wanxing.device.dto.request;

import cn.wanxing.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备列表查询请求（分页 + 筛选）
 */
@Getter
@Setter
public class DeviceQueryRequest extends PageRequest {

    /** 组织 ID（仅平台超管有效；机构管理员固定查本机构） */
    private Long orgId;

    /** 设备域 0 无人机 / 1 负载 / 2 遥控器 / 3 机场 */
    private Integer domain;

    /** 设备型号 */
    private Integer type;

    /** 设备子型号 */
    private Integer subType;

    /** 在线状态 ONLINE / OFFLINE */
    private String status;

    /** 关键字：模糊匹配设备 SN 或名称 */
    private String keyword;
}