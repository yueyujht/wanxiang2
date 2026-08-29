package cn.wanxing.user.service;

import cn.wanxing.common.log.ApiLog;
import cn.wanxing.user.constant.UserRoleEnum;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.dto.vo.RoleVO;
import cn.wanxing.user.entity.Role;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.mapper.RoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    private final UserContext userContext;

    /**
     * 角色列表（机构隔离，SQL 层过滤）：
     */
    @ApiLog("角色列表")
    public List<RoleVO> list() {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (operator.getOrgId() != null) {
            wrapper.and(w -> w
                    .isNull(Role::getOrgId).ne(Role::getCode, UserRoleEnum.SUPER_ADMIN.name())
                    .or()
                    .eq(Role::getOrgId, operator.getOrgId()));
        }
        return roleMapper.selectList(wrapper).stream().map(RoleVO::from).toList();
    }
}
