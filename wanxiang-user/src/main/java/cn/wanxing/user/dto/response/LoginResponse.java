package cn.wanxing.user.dto.response;

import cn.wanxing.user.dto.vo.UserVO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录响应（含 token）
 */
@Getter
@Setter
@Builder
public class LoginResponse {

    /** token 名称（请求头 key） */
    private String tokenName;

    /** token 值 */
    private String tokenValue;

    /** 用户信息 */
    private UserVO user;
}