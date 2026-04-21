package com.smartnote.dto.share;

import com.smartnote.entity.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公开笔记视图对象 VO
 *
 * 用途：展示公开分享的笔记完整信息（无需登录即可访问）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicNoteVO {

    private Long id;//笔记ID

    private String title;//笔记标题

    private String content;//完整正文内容

    private LocalDateTime createTime;//创建时间

    private LocalDateTime updateTime;//最后更新时间

    /**
     * 从Note实体对象构建PublicNoteVO
     *
     * @param note 笔记实体对象
     * @return 构建好的PublicNoteVO对象
     */
    public static PublicNoteVO fromEntity(Note note) {
        return PublicNoteVO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createTime(note.getCreateTime())
                .updateTime(note.getUpdateTime())
                .build();
    }
}
