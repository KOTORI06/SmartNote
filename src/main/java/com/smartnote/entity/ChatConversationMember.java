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
@TableName("chat_conversation_member")
public class ChatConversationMember {

    @TableId(type = IdType.AUTO)
    private Long id;//主键ID

    private Long conversationId;//会话ID

    private Long userId;//用户ID

    private LocalDateTime joinedAt;//加入时间
}