package cn.wanxing.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置：注册全局鉴权拦截器
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 放行发送验证码/登录（及 Spring 错误页），其余接口需登录
            SaRouter.match("/**")
                    .notMatch("/auth/sendCode", "/auth/login", "/error")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}