package cn.wanxing.user.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.wanxing.user.constant.EnableStatusEnum;
import cn.wanxing.user.constant.UserStateEnum;
import cn.wanxing.user.entity.Role;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.mapper.RoleMapper;
import cn.wanxing.user.mapper.RolePermissionMapper;
import cn.wanxing.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源：为 @SaCheckRole / @SaCheckPermission 提供角色与权限
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Role role = resolveRole(loginId);
        return role == null
                ? Collections.emptyList()
                : rolePermissionMapper.selectPermissionsByRoleId(role.getId());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Role role = resolveRole(loginId);
        return role == null
                ? Collections.emptyList()
                : Collections.singletonList(role.getCode());
    }

    /**
     * 查出用户当前「启用」的角色；用户不存在、未分配角色或角色被停用时返回 null
     */
    private Role resolveRole(Object loginId) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null || user.getRole() == null || user.getStatus() != UserStateEnum.ENABLED) {
            return null;
        }
        Role role = roleMapper.selectByCodeAndOrg(user.getRole(), user.getOrgId());
        if (role == null || role.getStatus() != EnableStatusEnum.ENABLED) {
            return null;
        }
        return role;
    }
} 
