package cn.wanxing.user.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.wanxing.common.log.ApiLog;
import cn.wanxing.user.constant.EnableStatusEnum;
import cn.wanxing.user.dto.request.CreateOrgRequest;
import cn.wanxing.user.dto.vo.OrgVO;
import cn.wanxing.user.entity.Org;
import cn.wanxing.user.entity.User;
import cn.wanxing.user.exception.UserErrorCode;
import cn.wanxing.user.exception.UserException;
import cn.wanxing.user.mapper.OrgMapper;
import cn.wanxing.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 机构（租户）服务
 */
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgMapper orgMapper;

    private final UserMapper userMapper;

    /**
     * 机构列表
     */
    @ApiLog("机构列表")
    public List<OrgVO> list() {
        return orgMapper.selectList(null).stream().map(OrgVO::from).toList();
    }

    /**
     * 创建机构，返回新机构 id
     */
    @ApiLog("创建机构")
    public OrgVO create(CreateOrgRequest req) {
        checkNameUnique(req.getName(), null);
        checkCodeUnique(req.getCode(), null);

        Org org = Org.createOrg(req);
        Assert.isTrue(orgMapper.insert(org) > 0, () -> new UserException(UserErrorCode.INSERT_FAILED));
        return OrgVO.from(org);
    }

    /**
     * 更新机构（名称/编码/描述）
     */
    @ApiLog("更新机构")
    public Boolean update(Long id, CreateOrgRequest req) {
        Org org = orgMapper.selectById(id);
        if (org == null) {
            throw new UserException(UserErrorCode.ORG_NOT_FOUND);
        }
        checkNameUnique(req.getName(), id);
        checkCodeUnique(req.getCode(), id);

        Org update = Org.updateOrg(org,req);
        Assert.isTrue(orgMapper.updateById(update) > 0, () -> new UserException(UserErrorCode.UPDATE_FAILED));

        // 机构被停用时，踢出该机构下所有已登录用户的 token
        if (update.getStatus() == EnableStatusEnum.DISABLED) {
            kickoutOrgUsers(id);
        }
        return Boolean.TRUE;
    }

    /**
     * 查询机构信息
     */
    @ApiLog("查询机构")
    public OrgVO getById(Long id) {
        Org org = orgMapper.selectById(id);
        if (org == null) {
            throw new UserException(UserErrorCode.ORG_NOT_FOUND);
        }
        return OrgVO.from(org);
    }

    private void checkNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Org> qw = new LambdaQueryWrapper<Org>().eq(Org::getName, name);
        if (excludeId != null) {
            qw.ne(Org::getId, excludeId);
        }
        if (orgMapper.selectCount(qw) > 0) {
            throw new UserException(UserErrorCode.ORG_NAME_EXISTS);
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Org> qw = new LambdaQueryWrapper<Org>().eq(Org::getCode, code);
        if (excludeId != null) {
            qw.ne(Org::getId, excludeId);
        }
        if (orgMapper.selectCount(qw) > 0) {
            throw new UserException(UserErrorCode.ORG_CODE_EXISTS);
        }
    }

    /**
     * 机构停用时，登出该机构下所有用户（使其 token 立即失效）
     */
    private void kickoutOrgUsers(Long orgId) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getOrgId, orgId));
        users.forEach(u -> StpUtil.logout(u.getId()));
    }
}
