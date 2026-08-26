package cn.wanxing.common.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 分页请求
 */
@Setter
@Getter
public class PageRequest extends BaseRequest {
    private static final long serialVersionUID = 1L;
    /**
     * 当前页（从 1 开始）
     */
    @Min(value = 1, message = "页码从 1 开始")
    private int currentPage = 1;

    /**
     * 每页结果数
     */
    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private int pageSize = 10;
}