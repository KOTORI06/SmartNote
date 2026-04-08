package com.smartnote.dto.ai;

import jakarta.validation.constraints.NotBlank;
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
    private String query;

    private String context; // 上下文信息

    private Map<String, Object> preferences; // 用户偏好
}
