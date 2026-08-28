package cn.wanxing.user.service;

import cn.wanxing.common.log.ApiLog;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.wanxing.user.constant.UserStateEnum;
import cn.wanxing.user.converter.UserConvert;
import cn.wanxing.user.dto.request.PhoneLoginRequest;
import cn.wanxing.user.dto.request.SendCodeRequest;
import cn.wanxing.user.dto.response.LoginResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务：发送验证码 / 手机号登录 / 登出 / 当前用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 验证码 Redis key 前缀 */
    private static final String SMS_CODE_KEY = "wanxiang:sms:code:";

    /** mock 固定验证码（真实短信网关接入后替换） */
    private static final String MOCK_CODE = "123456";

    /** 验证码有效期 */
    private static final Duration CODE_TTL = Duration.ofMinutes(3);

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final OrgMapper orgMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发送登录验证码：手机号必须已后台录入，未录入不发送
     */
    @ApiLog("发送登录验证码")
    public Boolean sendCode(SendCodeRequest req) {
        // 1.获取用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null) {
            throw new UserException(UserErrorCode.PHONE_NOT_REGISTERED);
        }

        // 2.校验帐号状态
        if (user.getStatus() != UserStateEnum.ENABLED) {
            throw new UserException(UserErrorCode.ACCOUNT_DISABLED);
        }

        // 3.校验机构状态：机构停用后全员不可登录
        checkOrgEnabled(user.getOrgId());

        // 4.将验证码存到redis，过期时间
        stringRedisTemplate.opsForValue().set(SMS_CODE_KEY + req.getPhone(), MOCK_CODE, CODE_TTL);
        log.info("【mock】向手机号 {} 发送验证码：{}", req.getPhone(), MOCK_CODE);
        return Boolean.TRUE;
    }

    /**
     * 手机号 + 验证码登录
     */
    @ApiLog("登录")
    public LoginResponse login(PhoneLoginRequest req) {
        // 1.根据手机号获取用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null) {
            throw new UserException(UserErrorCode.LOGIN_FAILED);
        }

        // 2.校验验证码
        String cached = stringRedisTemplate.opsForValue().get(SMS_CODE_KEY + req.getPhone());
        if (cached == null || !cached.equals(req.getCode())) {
            throw new UserException(UserErrorCode.CODE_INVALID);
        }

        // 3.校验账号状态：仅启用可登录
        if (user.getStatus() != UserStateEnum.ENABLED) {
            throw new UserException(UserErrorCode.ACCOUNT_DISABLED);
        }

        // 4.校验机构状态：机构停用后全员不可登录
        checkOrgEnabled(user.getOrgId());

        // 5.删除使用过的验证码，签发 token
        stringRedisTemplate.delete(SMS_CODE_KEY + req.getPhone());
        StpUtil.login(user.getId());

        // 5.更新User：最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        Assert.isTrue(userMapper.updateById(user)>0, () -> new UserException(UserErrorCode.UPDATE_FAILED));

        return LoginResponse.builder()
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .user(toVO(user))
                .build();
    }

    /**
     * 退出登录
     */
    @ApiLog("退出登录")
    public Boolean logout() {
        StpUtil.logout();
        return Boolean.TRUE;
    }

    /**
     * 获取当前登录用户
     */
    @ApiLog("查询当前用户")
    public UserVO currentUser() {
        User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        Role role = roleMapper.selectByCodeAndOrg(user.getRole(), user.getOrgId());
        return UserConvert.toVO(user, role,
                user.getOrgId() != null ? orgMapper.selectById(user.getOrgId()) : null);
    }

    /**
     * 校验机构是否启用：平台超管（无机构）不受限；机构停用则全员不可登录
     */
    private void checkOrgEnabled(Long orgId) {
        if (orgId == null) {
            return;
        }
        Org org = orgMapper.selectById(orgId);
        if (org == null) {
            throw new UserException(UserErrorCode.ORG_NOT_FOUND);
        }
        if (!org.isEnabled()) {
            throw new UserException(UserErrorCode.ORG_DISABLED);
        }
    }
}
