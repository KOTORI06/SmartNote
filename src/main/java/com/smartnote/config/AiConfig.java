package com.smartnote.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 核心配置类
 */
@Configuration
public class AiConfig {

    private final MethodToolCallbackProvider toolCallbackProvider;

    /**
     * 构造函数注入工具回调提供者
     *
     * Spring 会自动从容器中获取 ToolConfig 注册的 toolCallbackProvider
     *
     * @param toolCallbackProvider 工具回调提供者
     */
    public AiConfig(MethodToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * 配置 ChatClient 并启用工具调用
     *
     * 关键配置：
     * - .defaultTools(toolCallbackProvider) 注册所有可用工具
     * - AI 模型会根据用户意图自动选择是否调用工具
     *
     * @param builder ChatClient 构建器（由 Spring AI 自动提供）
     * @return 配置好的 ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                // 注册工具回调提供者，使 AI 能够调用 AiTools 中的方法
                .defaultTools(toolCallbackProvider)
                .build();
    }

}