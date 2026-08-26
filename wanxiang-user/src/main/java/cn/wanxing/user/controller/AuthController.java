package cn.wanxing.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.wanxing.common.result.Result;
import cn.wanxing.user.dto.request.PhoneLoginRequest;
import cn.wanxing.user.dto.request.SendCodeRequest;
import cn.wanxing.user.dto.response.LoginResponse;
import cn.wanxing.user.dto.vo.UserVO;
import cn.wanxing.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 发送登录验证码（手机号需已后台录入）
     */
    @PostMapping("/sendCode")
    public Result<Boolean> sendCode(@Valid @RequestBody SendCodeRequest req) {
        log.info("手机号{}请求发送验证码", req.getPhone());
        Boolean response = authService.sendCode(req);
        return Result.success(response);
    }

    /**
     * 手机号 + 验证码登录，返回 token
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody PhoneLoginRequest req) {
        log.info("手机号{}正在登陆",req.getPhone());
        LoginResponse response = authService.login(req);
        log.info("手机号{}登陆成功",req.getPhone());
        return Result.success(response);
    }

    /**
     * 登出（需登录）
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        log.info("用户{}退出登录", StpUtil.getLoginId());
        Boolean response = authService.logout();
        return Result.success(response);
    }

    /**
     * 当前登录用户（需登录）
     */
    @SaCheckLogin
    @GetMapping("/userInfo")
    public Result<UserVO> userInfo() {
        UserVO userVO = authService.currentUser();
        return Result.success(userVO);
    }
}
