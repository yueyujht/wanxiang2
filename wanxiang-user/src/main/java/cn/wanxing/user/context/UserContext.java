package cn.wanxing.user.context;

import cn.dev33.satoken.stp.StpUtil;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.exception.UserErrorCode;
import cn.wanxing.user.exception.UserException;
import cn.wanxing.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户上下文：统一提供「我是谁 + 所属机构」的判断，收口租户隔离逻辑
 */
@Component
@RequiredArgsConstructor
public class UserContext {

    private final UserMapper userMapper;

    public Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    public User currentUser() {
        User user = userMapper.selectById(userId());
        if (user == null) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 是否平台超管（无机构归属，org_id 为 null）
     */
    public boolean isSuperAdmin(User user) {
        return user.getOrgId() == null;
    }
}
