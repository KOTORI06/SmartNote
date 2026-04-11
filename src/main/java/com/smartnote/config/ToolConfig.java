package com.smartnote.config;

import com.smartnote.util.AiTools;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 工具配置类
 *
 * 功能说明：
 * 1. 将 AiTools 中的方法注册为 Spring AI 可识别的工具回调
 * 回调（你提供一个函数给别人，让别人在合适的时机调用它）
 * 2. 使 AI 模型能够在对话过程中调用这些工具
 *
 * 技术原理：
 * - MethodToolCallbackProvider 扫描 AiTools 中的所有公共方法
 * - 自动生成工具元数据（名称、描述、参数 schema）
 * - 注册到 Spring 容器，供 ChatClient 使用
 *
 * @author SmartNote Team
 */
@Configuration
public class ToolConfig {

    /**
     * 注册工具回调提供者
     *
     * MethodToolCallbackProvider 会自动扫描 AiTools 中的所有公共方法
     * 并根据方法签名生成工具定义
     *
     * @param aiTools AI 工具类实例（由 Spring 自动注入）
     * @return 工具回调提供者
     */
    @Bean
    public MethodToolCallbackProvider toolCallbackProvider(AiTools aiTools) {
        // 创建 MethodToolCallbackProvider
        // 它会将 AiTools 中的所有公共方法转换为 AI 可调用的工具
        return MethodToolCallbackProvider.builder()
                .toolObjects(aiTools)  // 指定包含工具方法的对象
                .build();
    }
}
