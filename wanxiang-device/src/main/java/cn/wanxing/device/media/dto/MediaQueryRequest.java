package cn.wanxing.device.media.dto;

import cn.wanxing.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 媒体文件查询条件
 */
@Getter
@Setter
public class MediaQueryRequest extends PageRequest {

    /** 文件名称模糊搜索 */
    private String fileName;

    /** 任务 ID 筛选 */
    private String flightId;
}
