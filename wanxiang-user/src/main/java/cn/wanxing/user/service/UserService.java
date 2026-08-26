package cn.wanxing.user.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.wanxing.common.request.PageRequest;
import cn.wanxing.user.constant.EnableStatusEnum;
import cn.wanxing.user.constant.UserRoleEnum;
import cn.wanxing.user.constant.UserStateEnum;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.converter.UserConvert;
import cn.wanxing.user.dto.request.CreateUserRequest;
import cn.wanxing.user.dto.response.UserPageResponse;
import cn.wanxing.user.dto.vo.UserVO;
import cn.wanxing.user.entity.Org;
import cn.wanxing.user.entity.Role;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.exception.UserErrorCode;
import cn.wanxing.user.exception.UserException;
import cn.wanxing.user.mapper.OrgMapper;
import cn.wanxing.user.mapper.RoleMapper;
import cn.wanxing.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.wanxing.user.exception.UserErrorCode.OPERATION_FORBIDDEN;
import static cn.wanxing.user.exception.UserErrorCode.UPDATE_FAILED;

/**
 * 用户管理服务（租户隔离：机构管理员只能操作本机构用户，平台超管可跨机构）
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final OrgMapper orgMapper;

    private final UserContext userContext;

    /**
     * 用户分页列表：机构管理员只见本机构，超管可传 orgId 过滤（不传则全量）
     */
    public UserPageResponse<UserVO> page(PageRequest req, Long orgId) {
        // 1.获取操作用户
        User operator = userContext.currentUser();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        // 2.校验、查询
        if (operator.getOrgId() != null) {
            qw.eq(User::getOrgId, operator.getOrgId());
        } else if (orgId != null) {
            qw.eq(User::getOrgId, orgId);
        }
        Page<User> page = userMapper.selectPage(new Page<>(req.getCurrentPage(), req.getPageSize()), qw);
        return UserPageResponse.of(
                toVOList(page.getRecords()), page.getTotal(), req.getPageSize(), req.getCurrentPage());
    }

    /**
     * 后台建账号（录入手机号）：机构管理员只能建在本机构，超管需指定机构
     */
    public UserVO create(CreateUserRequest req) {
        // 1.获取操作用户
        User operator = userContext.currentUser();
        // 2.解析组织id
        Long orgId = resolveOrgId(operator, req.getOrgId());
        // 3.获取初始化角色
        Role role = checkUserRole(req.getRole(), orgId);
        // 4.校验
        if (UserRoleEnum.isSuperAdmin(req.getRole())) {
            throw new UserException(OPERATION_FORBIDDEN);
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (count > 0) {
            throw new UserException(UserErrorCode.PHONE_EXISTS);
        }

        // 5.初始化账号
        User user = User.createUser(req, orgId);
        Assert.isTrue(userMapper.insert(user) > 0,() -> new UserException(UserErrorCode.INSERT_FAILED));
        return UserConvert.toVO(user, role, orgMapper.selectById(orgId));
    }

    /**
     * 分配角色（仅本机构范围内，且不可分配 super_admin）
     */
    public Boolean assignRole(Long userId, String role) {
        // 1.查询、校验操作员
        User operator = userContext.currentUser();
        if (Objects.equals(userId, operator.getId())) {
            throw new UserException(UserErrorCode.CANNOT_OPERATE_SELF);
        }

        // 2.查询、校验目标用户
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), target.getOrgId())) {
            throw new UserException(OPERATION_FORBIDDEN);
        }

        // 3.校验用户角色（目标用户所在机构范围内）
        checkUserRole(role, target.getOrgId());

        // 4.更新User：role
        target.setRole(role);
        Assert.isTrue(userMapper.updateById(target) != 0,() -> new UserException(UPDATE_FAILED));
        return Boolean.TRUE;
    }

    /**
     * 修改状态（启用/禁用/锁定），仅本机构范围内，不允许操作自己
     */
    public Boolean changeStatus(Long userId, String statusCode) {
        // 1.获取、校验操作员
        User operator = userContext.currentUser();
        if (Objects.equals(userId, operator.getId())) {
            throw new UserException(UserErrorCode.CANNOT_OPERATE_SELF);
        }

        // 2.获取、校验目标用户
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), target.getOrgId())) {
            throw new UserException(OPERATION_FORBIDDEN);
        }

        // 3.更新User：state
        target.setStatus(UserStateEnum.valueOf(statusCode));
        Assert.isTrue(userMapper.updateById(target) > 0,() -> new UserException(UPDATE_FAILED));

        // 4.删除失效用户的token
        if(statusCode.equals(UserStateEnum.DISABLED.name())) {
            StpUtil.logout(userId);
        }
        return Boolean.TRUE;
    }

    /**
     * 解析目标机构：机构管理员强制本机构，超管需指定并校验存在
     */
    private Long resolveOrgId(User operator, Long requestedOrgId) {
        Long orgId = operator.getOrgId() != null ? operator.getOrgId() : requestedOrgId;
        if (orgId == null) {
            throw new UserException(UserErrorCode.ORG_NOT_FOUND);
        }
        Org org = orgMapper.selectById(orgId);
        if (org == null) {
            throw new UserException(UserErrorCode.ORG_NOT_FOUND);
        }
        if (!org.isEnabled()) {
            throw new UserException(UserErrorCode.ORG_DISABLED);
        }
        return orgId;
    }

    /**
     * 校验角色
     */
    private Role checkUserRole(String roleCode, Long orgId) {
        // 按 code + 机构解析：全局角色或本机构自定义角色，其他机构的角色解析不到（隐式机构隔离）
        Role role = roleMapper.selectByCodeAndOrg(roleCode, orgId);
        if (role == null) {
            throw new UserException(UserErrorCode.ROLE_NOT_FOUND);
        }
        if (UserRoleEnum.isSuperAdmin(roleCode)) {
            throw new UserException(OPERATION_FORBIDDEN);
        }
        if (role.getStatus() != EnableStatusEnum.ENABLED) {
            throw new UserException(UserErrorCode.ROLE_DISABLED);
        }
        return role;
    }

    private List<UserVO> toVOList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        // 角色数量少，一次加载全部，按 (code, org) 匹配
        List<Role> allRoles = roleMapper.selectList(null);

        Set<Long> orgIds = users.stream()
                .map(User::getOrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Org> orgMap = orgIds.isEmpty()
                ? Collections.emptyMap()
                : orgMapper.selectBatchIds(orgIds).stream()
                        .collect(Collectors.toMap(Org::getId, Function.identity()));

        return users.stream()
                .map(user -> {
                    Role role = allRoles.stream()
                            .filter(r -> r.getCode().equals(user.getRole()))
                            .filter(r -> r.getOrgId() == null || r.getOrgId().equals(user.getOrgId()))
                            .findFirst().orElse(null);
                    return UserConvert.toVO(user, role, orgMap.get(user.getOrgId()));
                })
                .toList();
    }
}
