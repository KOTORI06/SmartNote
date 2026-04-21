package com.smartnote.dto.chat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotNull(message = "会话类型不能为空")
    private Integer type;//会话类型：1-私聊，2-群聊

    private String name;//会话名称（群聊必填，私聊可为空）

    @NotEmpty(message = "成员列表不能为空")
    private List<Long> memberIds;//成员列表
}
