package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//笔记浏览历史记录表实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("note_view_history")
public class NoteViewHistory {

    @TableId(type = IdType.AUTO)
    private Long id;//浏览记录唯一主键
    private Long userId;//浏览用户ID（逻辑外键，关联user.id）
    private Long noteId;//被浏览的笔记ID（逻辑外键，关联note.id）
    private LocalDateTime viewTime;//浏览时间
}
