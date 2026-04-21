package com.smartnote.dto.note;

import com.smartnote.entity.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记列表视图对象 VO
 *
 * 用途：在笔记列表中展示每条笔记的摘要信息
 * 特点：包含基本信息、标签列表和AI分析摘要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteVO {

    private Long id;//笔记ID

    private String title;//笔记标题

    private String contentPreview;//内容预览（截取前100字符）

    private Long folderId;//所属文件夹ID

    private List<TagVO> tags;//关联的标签列表

    private String aiSummary;//AI生成的摘要预览（如果有）

    private LocalDateTime createTime;//创建时间

    private LocalDateTime updateTime;//最后更新时间

    /**
     * 从完整内容中截取预览文本（默认100字符）
     *
     * @param content 完整的笔记内容
     * @return 截取后的预览文本，如果超过100字符则添加"..."
     */
    public static String turnContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        if (content.length() <= 100) {
            return content;
        }

        return content.substring(0, 100) + "...";
    }

    /**
     * 从AI分析结果中提取摘要预览（默认50字符）
     *
     * @param aiSummary 完整的AI摘要内容
     * @return 截取后的摘要预览，如果超过50字符则添加"..."
     */
    public static String turnAiSummary(String aiSummary) {
        if (aiSummary == null || aiSummary.isEmpty()) {
            return "";
        }

        if (aiSummary.length() <= 50) {
            return aiSummary;
        }

        return aiSummary.substring(0, 50) + "...";
    }

    /**
     * 从Note实体对象构建NoteVO
     *
     * @param note 笔记实体对象
     * @param tags 标签列表
     * @param aiSummary AI摘要内容（会自动截取为50字符预览）
     * @return 构建好的NoteVO对象
     */
    public static NoteVO fromEntity(Note note, List<TagVO> tags, String aiSummary) {
        return NoteVO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .contentPreview(turnContent(note.getContent()))
                .folderId(note.getFolderId())
                .tags(tags)
                .aiSummary(turnAiSummary(aiSummary))
                .createTime(note.getCreateTime())
                .updateTime(note.getUpdateTime())
                .build();
    }
}
