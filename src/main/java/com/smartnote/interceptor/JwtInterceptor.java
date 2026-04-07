package com.smartnote.interceptor;

import com.smartnote.util.JwtUtil;
import com.smartnote.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * JWT 认证拦截器
 *
 * 功能：
 * 1. 拦截请求，验证 Token 有效性
 * 2. 自动刷新即将过期的 Token
 * 3. 将用户信息存入请求上下文，供后续使用
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    /**
     * JSON 对象映射器，用于序列化响应数据
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Token 刷新阈值：30 分钟
     * 当 Token 剩余有效期小于 30 分钟时，自动生成新 Token
     */
    private static final int REFRESH_THRESHOLD_MINUTES = 30;

    /**
     * 预处理方法，在 Controller 执行前调用
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler 被调用的处理器（Controller 方法）
     * @return true-放行请求，false-拦截请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取 Authorization 字段
        String authHeader = request.getHeader(JwtUtil.HEADER_STRING);

        // 2. 提取 Token（去除 "Bearer " 前缀）
        String token = JwtUtil.extractTokenFromHeader(authHeader);

        // 3. 检查 Token 是否存在
        if (token == null || token.isEmpty()) {
            log.warn("请求未携带 Token，路径：{}", request.getRequestURI());
            sendErrorResponse(response, 401, "未提供认证令牌");
            return false;
        }

        // 4. 验证 Token 是否有效（签名正确且未过期）
        if (!JwtUtil.validateToken(token)) {
            log.warn("Token 无效或已过期，路径：{}", request.getRequestURI());
            sendErrorResponse(response, 401, "认证令牌无效或已过期，请重新登录");
            return false;
        }

        // 5. 检查 Token 是否即将过期，如果是则生成新 Token
        if (JwtUtil.isTokenExpiringSoon(token, REFRESH_THRESHOLD_MINUTES)) {
            try {
                // 刷新 Token，生成新的 Token
                String newToken = JwtUtil.refreshToken(token);

                // 将新 Token 放入响应头，前端收到后更新本地存储
                response.setHeader("New-Token", newToken);
                // 允许前端访问自定义响应头
                response.setHeader("Access-Control-Expose-Headers", "New-Token");

                log.info("Token 即将过期，已为用户生成新 Token");
            } catch (Exception e) {
                log.error("刷新 Token 失败", e);
                // 刷新失败不影响当前请求，用户下次需要重新登录
            }
        }

        // 6. 从 Token 中解析用户信息
        Long userId = JwtUtil.getUserIdFromToken(token);
        String username = JwtUtil.getUsernameFromToken(token);

        // 7. 将用户信息存入请求属性，供 Controller 和 Service 层使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        log.debug("用户 [{}] (ID: {}) 通过认证，访问路径：{}", username, userId, request.getRequestURI());

        // 8. 放行请求，继续执行 Controller 方法
        return true;
    }

    /**
     * 发送错误响应
     *
     * @param response HTTP 响应对象
     * @param code 错误状态码
     * @param message 错误消息
     * @throws IOException IO 异常
     */
    private void sendErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        // 设置响应状态码
        response.setStatus(code);
        // 设置响应内容类型为 JSON
        response.setContentType("application/json;charset=UTF-8");

        // 构建统一响应结果
        Result<Void> result = Result.error(code, message);

        // 将结果序列化为 JSON 并写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
