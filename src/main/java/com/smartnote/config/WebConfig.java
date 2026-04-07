package com.smartnote.config;

import com.smartnote.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 Web MVC 配置类
 用于注册拦截器、配置跨域等
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     添加拦截器配置

     @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有 /api/ 开头的请求
                .addPathPatterns("/api/**")
                // 排除不需要认证的接口
                .excludePathPatterns(
                        "/api/users/login",      // 登录接口
                        "/api/users/register"    // 注册接口
                );
    }
}
