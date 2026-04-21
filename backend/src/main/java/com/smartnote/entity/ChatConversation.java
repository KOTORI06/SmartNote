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
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.AUTO)
    private Long id;//会话ID

    private Integer type;//会话类型：1-私聊，2-群聊

    private String name;//会话名称（群聊必填，私聊可为空）

    private String avatar;//会话头像URL

    private Long ownerId;//创建者ID

    private LocalDateTime createdAt;//创建时间

    private LocalDateTime updatedAt;//更新时间

    private Long lastMessageId;//最后一条消息ID

    private Integer isDeleted;//是否逻辑删除：0-否，1-是
}