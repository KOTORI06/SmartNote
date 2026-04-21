package com.smartnote.dto.ai;

import com.smartnote.entity.ChatSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {

    private Long id;//会话id

    private String sessionName;//会话名称

    private LocalDateTime createTime;//创建时间

    private LocalDateTime updateTime;//更新时间

    /**
     * 从 ChatSession 实体对象构建 ChatSessionVO
     *
     * @param session 会话实体对象
     * @return 构建好的 ChatSessionVO 对象
     */
    public static ChatSessionVO fromEntity(ChatSession session) {
        return ChatSessionVO.builder()
                .id(session.getId())//会话id
                .sessionName(session.getSessionName())//会话名称
                .createTime(session.getCreateTime())//创建时间
                .updateTime(session.getUpdateTime())//更新时间
                .build();
    }
}
