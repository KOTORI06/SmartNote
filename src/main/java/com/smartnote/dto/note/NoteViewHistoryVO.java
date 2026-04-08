package com.smartnote.dto.note;

import com.smartnote.entity.Note;
import com.smartnote.entity.NoteViewHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记浏览历史视图对象 VO
 *
 * 用途：展示用户最近浏览的笔记记录
 * 特点：包含笔记基本信息和浏览时间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteViewHistoryVO {

    private Long noteId;//笔记ID

    private String title;//笔记标题

    private String contentPreview;//内容预览

    private LocalDateTime viewTime;//浏览时间

    private LocalDateTime noteUpdateTime;//笔记最后更新时间（用于判断是否有新内容）

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
     * 从NoteViewHistory和Note实体对象构建NoteViewHistoryVO
     *
     * @param history 浏览历史记录实体
     * @param note 笔记实体对象（提供标题、内容预览、更新时间）
     * @return 构建好的NoteViewHistoryVO对象
     */
    public static NoteViewHistoryVO fromEntity(NoteViewHistory history, Note note) {
        return NoteViewHistoryVO.builder()
                .noteId(note.getId())
                .title(note.getTitle())
                .contentPreview(turnContent(note.getContent()))
                .viewTime(history.getViewTime())
                .noteUpdateTime(note.getUpdateTime())
                .build();
    }
}
