package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//笔记与标签的关联关系表实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("note_tag")
public class NoteTag {

    @TableId(type = IdType.AUTO)
    private Long id;//关联关系唯一主键
    private Long noteId;//笔记ID（逻辑外键，关联note.id）
    private Long tagId;//标签ID（逻辑外键，关联tag.id）
    private LocalDateTime createTime;//关联创建时间
}
