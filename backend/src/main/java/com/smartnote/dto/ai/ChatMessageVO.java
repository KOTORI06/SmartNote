package com.smartnote.dto.ai;

import com.smartnote.entity.ChatMessage;
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

    private Long id;//消息id

    private String role;//user-用户，assistant-AI

    private String content;//消息内容

    private LocalDateTime createTime;//创建时间

    /**
     * 从 ChatMessage 实体对象构建 ChatMessageVO
     *
     * @param message 聊天消息实体对象
     * @return 构建好的 ChatMessageVO 对象
     */
    public static ChatMessageVO fromEntity(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createTime(message.getCreateTime())
                .build();
    }
}
