package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//笔记AI智能分析结果记录表实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_analysis")
public class AiAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;//分析记录唯一主键
    private Long noteId;//被分析的笔记ID（逻辑外键，关联note.id）
    private String analysisType;//分析类型，如 SUMMARY, KEY_POINTS, TAGS
    private String analysisContent;//AI分析生成的内容结果
    private String promptUsed;//本次分析使用的完整Prompt
    private String modelUsed;//使用的AI模型名称
    private String tokenUsage;//本次调用消耗的token情况（JSON格式）
    private LocalDateTime createTime;//分析请求时间
}
