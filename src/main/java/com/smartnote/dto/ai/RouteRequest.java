package com.smartnote.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 智能路由请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {

    @NotBlank(message = "查询内容不能为空")
    @Size(max = 2000, message = "查询内容不能超过2000个字符")
    private String query;//查询内容

    private Long sessionId;//会话id
}
