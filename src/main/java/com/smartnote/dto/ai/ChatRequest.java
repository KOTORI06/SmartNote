package com.smartnote.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI对话请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000个字符")
    private String message;

    private String conversationId; // 会话ID，用于多轮对话

    private String model; // 指定AI模型

    private Double temperature; // 温度参数

    private Integer maxTokens; // 最大token数
}
