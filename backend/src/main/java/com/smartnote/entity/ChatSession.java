package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;//会话ID

    private Long userId;//用户ID

    private String sessionName;//会话名称

    private LocalDateTime createTime;//创建时间

    private LocalDateTime updateTime;//更新时间

    private Integer isDeleted;//是否删除
}
