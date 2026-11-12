package com.etm.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置：
 * 1) 第一个拦截器做 URL 级登录校验（/api/** 全部要求登录，登录接口除外）
 * 2) 第二个拦截器开启注解鉴权（@SaCheckLogin / @SaCheckRole）
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/error",
            ".api/ai/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle ->
                        SaRouter.match("/api/**")
                                .notMatch(WHITE_LIST)
                                .check(r -> StpUtil.checkLogin())
                ))
                .addPathPatterns("/api/**")
                .order(1);

        // 开启注解鉴权：@SaCheckRole("ADMIN") 等
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/api/**")
                .order(2);
    }
}
