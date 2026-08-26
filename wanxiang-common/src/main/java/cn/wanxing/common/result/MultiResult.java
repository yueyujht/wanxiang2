package cn.wanxing.common.result;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static cn.wanxing.common.exception.CommonErrorCode.SUCCESS;

/**
 * 分页返回结果
 */
@Getter
@Setter
public class MultiResult<T> extends Result<List<T>> {
    /**
     * 总记录数
     */
    private long total;
    /**
     * 当前页码
     */
    private int page;
    /**
     * 每页记录数
     */
    private int size;

    public MultiResult() {
        super();
    }

    public MultiResult(String code, String message, List<T> data, long total, int page, int size) {
        super(code, message, data);
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> MultiResult<T> successMulti(List<T> data, long total, int page, int size) {
        return new MultiResult<>(SUCCESS.getCode(), SUCCESS.getMessage(), data, total, page, size);
    }

    public static <T> MultiResult<T> errorMulti(String errorCode, String errorMsg) {
        return new MultiResult<>(errorCode, errorMsg, null, 0, 0, 0);
    }

}