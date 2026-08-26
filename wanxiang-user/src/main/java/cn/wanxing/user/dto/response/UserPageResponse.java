package cn.wanxing.user.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UserPageResponse<T> {
    private static final long serialVersionUID = 1L;

    private List<T> datas;

    /**
     * 当前页
     */
    private int currentPage;
    /**
     * 每页结果数
     */
    private int pageSize;
    /**
     * 总数
     */
    private long total;

    public static <T> UserPageResponse<T> of(List<T> datas, long total, int pageSize,int currentPage) {
        UserPageResponse<T> pageResponse = new UserPageResponse<>();
        pageResponse.setDatas(datas);
        pageResponse.setTotal(total);
        pageResponse.setPageSize(pageSize);
        pageResponse.setCurrentPage(currentPage);
        return pageResponse;
    }
}
