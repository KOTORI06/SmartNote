package com.smartnote.dto.note;

import com.smartnote.dto.ai.AiAnalysisVO;
import com.smartnote.entity.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记详情视图对象 VO
 *
 * 用途：展示笔记的完整详细信息
 * 特点：包含完整内容、所有标签、完整的AI分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteDetailVO {

    private Long id;//笔记ID

    private Long userId;//所属用户ID

    private String title;//笔记标题

    private String content;//完整正文内容

    private Long folderId;//所属文件夹ID

    private List<TagVO> tags;//关联的标签列表

    private AiAnalysisVO aiAnalyses;//AI分析结果（只返回最新一条）（摘要、关键点、标签建议等）

    private LocalDateTime createTime;//创建时间

    private LocalDateTime updateTime;//最后更新时间

    /**
     * 从Note实体对象构建NoteDetailVO
     *
     * @param note 笔记实体对象
     * @param tags 标签列表
     * @param aiAnalysis AI分析结果对象
     * @return 构建好的NoteDetailVO对象
     */
    public static NoteDetailVO fromEntity(Note note, List<TagVO> tags, AiAnalysisVO aiAnalysis) {
        return NoteDetailVO.builder()
                .id(note.getId())
                .userId(note.getUserId())
                .title(note.getTitle())
                .content(note.getContent())
                .folderId(note.getFolderId())
                .tags(tags)
                .aiAnalyses(aiAnalysis)
                .createTime(note.getCreateTime())
                .updateTime(note.getUpdateTime())
                .build();
    }
}
