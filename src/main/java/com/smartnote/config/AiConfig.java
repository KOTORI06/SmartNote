package com.smartnote.config;

import com.smartnote.util.AiTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 核心配置类
 */
@Configuration
public class AiConfig {
    /**
     * 配置 ChatClient 并启用工具调用
     *
     * - AI 模型会根据用户意图自动选择是否调用工具
     *
     * @return 配置好的 ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel model,AiTools aiTools) {
        return ChatClient.builder(model)
                .defaultTools(aiTools)
                .build();
    }

}