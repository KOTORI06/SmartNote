package com.smartnote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置类
 *
 * 功能说明：
 * 1. 配置 HTTP 安全策略
 * 2. 禁用 CSRF 保护（因为使用 JWT 无状态认证）
 * 3. 设置会话管理为无状态模式
 * 4. 配置接口访问权限规则
 *
 * @author SmartNote Team
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置安全过滤链
     *
     * 核心配置项：
     * 1. CSRF 保护：禁用（JWT 不需要 CSRF token）
     * 2. 会话管理：STATELESS 无状态模式（不创建 HttpSession）
     * 3. 授权规则：
     *    - 公开接口：注册、登录、公开分享笔记
     *    - 其他接口：暂时全部放行（通过 JWT 拦截器进行实际鉴权）
     *
     * @param http HttpSecurity 构建器对象
     * @return 配置好的安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF 跨站请求伪造保护
                // 原因：使用 JWT Token 认证，不需要 Session，CSRF 攻击无法利用 Cookie
                .csrf(csrf -> csrf.disable())

                // 设置会话创建策略为无状态
                // STATELESS：Spring Security 不会创建或使用 HttpSession
                // 每次请求都需要携带 JWT Token 进行身份验证
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置 HTTP 请求的授权规则
                .authorizeHttpRequests(auth -> auth

                        // 允许匿名访问的公开接口
                        // /api/users/register - 用户注册接口
                        // /api/users/login - 用户登录接口
                        // /api/shares/public/** - 公开分享的笔记接口
                        .requestMatchers("/api/users/register", "/api/users/login", "/api/shares/public/**").permitAll()

                        // 其他所有请求也允许访问
                        // 注意：实际的权限校验由 JwtInterceptor 拦截器完成
                        // 这里放行是为了让请求能够到达拦截器进行 Token 验证
                        .anyRequest().permitAll()
                );
        // 构建并返回安全过滤链
        return http.build();
    }
}
