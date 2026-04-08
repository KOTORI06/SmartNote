package com.smartnote.dto.share;

import com.smartnote.entity.Note;
import com.smartnote.entity.NotePermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分享笔记视图对象 VO
 *
 * 用途：在分享列表中展示笔记摘要信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedNoteVO {

    private Long noteId;//笔记ID

    private String title;//笔记标题

    private String contentPreview;//内容预览（截取前100字符）

    private Long ownerId;//笔记拥有者ID

    private String ownerName;//笔记拥有者名称

    private Integer permissionType;//权限类型：1-可查看，2-可编辑

    private LocalDateTime shareTime;//分享时间

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
     * 从Note和NotePermission实体对象构建SharedNoteVO
     *
     * @param note 笔记实体对象
     * @param ownerName 笔记拥有者名称
     * @param permission 权限实体对象
     * @return 构建好的SharedNoteVO对象
     */
    public static SharedNoteVO fromEntity(Note note, String ownerName, NotePermission permission) {
        return SharedNoteVO.builder()
                .noteId(note.getId())
                .title(note.getTitle())
                .contentPreview(turnContent(note.getContent()))
                .ownerId(note.getUserId())
                .ownerName(ownerName)
                .permissionType(permission.getPermissionType())
                .shareTime(permission.getCreateTime())
                .build();
    }
}
