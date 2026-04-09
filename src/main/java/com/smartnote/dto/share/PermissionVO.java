package com.smartnote.dto.share;

import com.smartnote.entity.NotePermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限视图对象 VO
 *
 * 用途：展示笔记的分享权限信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {

    private Long id;//权限记录ID

    private Long noteId;//笔记ID

    private String noteTitle;//笔记标题

    private Integer granteeType;//被授权者类型：1-用户，2-好友分组，3-所有人

    private Long granteeId;//被授权者ID

    private Integer permissionType;//权限类型：1-可查看，2-可编辑

    private LocalDateTime createTime;//权限创建时间

    /**
     * 从NotePermission实体对象构建PermissionVO
     *
     * @param permission 权限实体对象
     * @param noteTitle 笔记标题
     * @return 构建好的PermissionVO对象
     */
    public static PermissionVO fromEntity(NotePermission permission, String noteTitle) {
        return PermissionVO.builder()
                .id(permission.getId())
                .noteId(permission.getNoteId())
                .noteTitle(noteTitle)
                .granteeType(permission.getGranteeType())
                .granteeId(permission.getGranteeId())
                .permissionType(permission.getPermissionType())
                .createTime(permission.getCreateTime())
                .build();
    }
}
