package com.smartnote.dto.ai;

import com.smartnote.entity.AiAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI分析结果响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisVO {

    private Long id;//分析记录ID

    private String analysisType;//分析类型（SUMMARY/KEY_POINTS/TAGS等）

    private String analysisContent;//分析结果内容

    private LocalDateTime createTime;//分析时间

    /**
     * 从AiAnalysis实体对象构建AiAnalysisVO
     *
     * @param aiAnalysis AI分析实体对象
     * @return 构建好的AiAnalysisVO对象
     */
    public static AiAnalysisVO fromEntity(AiAnalysis aiAnalysis) {
        return AiAnalysisVO.builder()
                .id(aiAnalysis.getId())
                .analysisType(aiAnalysis.getAnalysisType())
                .analysisContent(aiAnalysis.getAnalysisContent())
                .createTime(aiAnalysis.getCreateTime())
                .build();
    }
}