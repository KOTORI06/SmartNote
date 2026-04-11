package com.smartnote.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    @NotBlank(message = "会话名称不能为空")
    @Size(max = 20, message = "会话名称长度不能超过20个字符")
    private String sessionName;//会话名称
}
