package com.smartnote.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 静态方法直接用户类调用
 用户上下文工具类
 用于在 Service 层获取当前登录用户信息
 */
public class UserContext {

    /**
     获取当前登录用户ID

     @return 用户ID
     */
    public static Long getCurrentUserId() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            throw new RuntimeException("无法获取请求上下文");
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    /**
     获取当前登录用户名

     @return 用户名
     */
    public static String getCurrentUsername() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            throw new RuntimeException("无法获取请求上下文");
        }

        String username = (String) request.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("用户未登录");
        }
        return username;
    }

    /**
     获取 HttpServletRequest 对象
     */
    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
