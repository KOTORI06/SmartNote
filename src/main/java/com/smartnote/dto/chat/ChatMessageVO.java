package com.smartnote.dto.chat;

import com.smartnote.entity.ChatMessageNew;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    private Long id;// 消息ID

    private Long conversationId;// 会话ID

    private Long senderId;// 发送者ID

    private String senderName;// 发送者名称

    private Integer messageType;// 消息类型(1: 文本消息, 2: 图片消息)

    private String content;// 消息内容

    private String mediaUrl;// 媒体文件URL

    private Integer isRead;// 是否已读(0: 未读, 1: 已读)

    private LocalDateTime createdAt;// 创建时间

    /**
     * 将实体转换为VO
     *
     * @param message 消息实体
     * @param senderName 发送者名称
     * @return 聊天消息VO
     */
    public static ChatMessageVO fromEntity(ChatMessageNew message, String senderName) {
        return ChatMessageVO.builder()
                .id(message.getId())//消息ID
                .conversationId(message.getConversationId())//会话ID
                .senderId(message.getSenderId())//发送者ID
                .senderName(senderName)//发送者名称
                .messageType(message.getMessageType())//消息类型(1: 文本消息, 2: 图片消息)
                .content(message.getContent())//消息内容
                .mediaUrl(message.getMediaUrl())//媒体文件URL
                .isRead(message.getIsRead())//是否已读(0: 未读, 1: 已读)
                .createdAt(message.getCreatedAt())//创建时间
                .build();
    }
}
