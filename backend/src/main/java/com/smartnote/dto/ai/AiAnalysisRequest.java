package com.smartnote.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI分析请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisRequest {

    @NotBlank(message = "分析类型不能为空")
    private String analysisType; // SUMMARY, KEY_POINTS, TAGS, TRANSLATION, SENTIMENT等

    private String customPrompt; // 自定义Prompt
}
