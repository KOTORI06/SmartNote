package com.smartnote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类
 *
 * 功能说明：
 * 1. 配置并启用 WebSocket 支持
 * 2. 注册 ServerEndpointExporter Bean，自动扫描 @ServerEndpoint 注解
 *
 * - Spring Boot 默认不会自动扫描 WebSocket 端点
 * - 必须手动注册 ServerEndpointExporter 才能使用 @ServerEndpoint
 * - 否则 WebSocket 连接会失败（404 或连接拒绝）
 */

@Configuration
public class WebSocketConfig {

    /**
     * 注册 ServerEndpointExporter Bean
     *
     * 1. 扫描项目中所有带有 @ServerEndpoint 注解的类
     * 2. 将这些类注册为 WebSocket 端点
     * 3. 启用 WebSocket 服务，使其可以接收客户端连接
     *
     * 工作流程：
     * 1. Spring Boot 启动
     * 2. 加载 WebSocketConfig 配置类
     * 3. 创建 ServerEndpointExporter 实例
     * 4. 扫描 @ServerEndpoint 注解
     * 5. 注册 ChatWebSocket 为端点
     * 6. WebSocket 服务可用
     *
     * @return ServerEndpointExporter 实例，用于导出 WebSocket 端点
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
