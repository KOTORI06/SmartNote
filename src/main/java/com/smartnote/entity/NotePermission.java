package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//笔记分享权限表实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("note_permission")
public class NotePermission {

    @TableId(type = IdType.AUTO)
    private Long id;//权限记录唯一主键
    private Long noteId;//被分享的笔记ID（逻辑外键，关联note.id）
    private Long ownerId;//笔记拥有者ID（逻辑外键，关联user.id）
    private Integer granteeType;//被授权者类型：1-用户，2-好友分组，3-所有人
    private Long granteeId;//被授权者ID（当grantee_type=1时，逻辑外键关联user.id）
    private Integer permissionType;//权限类型：1-可查看，2-可编辑
    private LocalDateTime createTime;//权限创建时间
    private LocalDateTime updateTime;//权限更新时间
}
