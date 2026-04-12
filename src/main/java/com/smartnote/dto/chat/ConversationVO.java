package com.smartnote.dto.chat;

import com.smartnote.entity.ChatConversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private Long id;//会话ID

    private Integer type;//会话类型(1:私聊，2:群聊)

    private String name;//会话名称

    private String avatar;//头像URL

    private Long ownerId;//会话创建者ID

    private Integer memberCount;//会话成员数量

    private String lastMessageContent;//最后一条消息内容

    private LocalDateTime lastMessageTime;//最后一条消息时间

    /**
     * 从实体类转换成VO
     *
     * @param conversation
     * @return
     */
    public static ConversationVO fromEntity(ChatConversation conversation) {
        return ConversationVO.builder()
                .id(conversation.getId())//会话ID
                .type(conversation.getType())//会话类型(1:私聊，2:群聊)
                .name(conversation.getName())//会话名称
                .avatar(conversation.getAvatar())//头像URL
                .ownerId(conversation.getOwnerId())//会话创建者ID
                .build();
    }
}
