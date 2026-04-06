package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//笔记核心信息表实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("note")
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;//笔记ID(唯一主键)
    private Long userId;//笔记所属的用户ID（逻辑外键，关联user.id）
    private String title;//笔记标题
    private String content;//笔记正文内容
    private Long folderId;//笔记所属文件夹ID（预留，用于文件夹功能）
    private Boolean isDeleted;//逻辑删除标志：0-未删除，1-已删除
    private LocalDateTime createTime;//笔记创建时间
    private LocalDateTime updateTime;//笔记最后修改时间
}
