package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好有聊天消息实体类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_message_new")
public class ChatMessageNew {

    @TableId(type = IdType.AUTO)
    private Long id;//主键ID

    private Long conversationId;//会话ID

    private Long senderId;//发送者ID

    private Integer messageType;//消息类型(1: 文本消息, 2: 图片消息)

    private String content;//消息内容

    private String mediaUrl;//媒体文件URL

    private Integer isRead;//是否已读(0: 未读, 1: 已读)

    private LocalDateTime createdAt;//创建时间
}