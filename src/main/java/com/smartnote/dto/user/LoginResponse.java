package com.smartnote.dto.user;

import com.smartnote.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 登录响应 DTO

 用途：登录成功后返回给前端的数据
 特点：包含 JWT Token 和用户基本信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;//JWT 访问令牌
    private Long id;//用户ID
    private String username;//用户名
    private String avatarUrl;//头像图片URL地址
    private String motto;//座右铭或简介

    /**
     创建 LoginResponse 对象
     @param user 用户实体对象
     @param token JWT 访问令牌
     @return LoginResponse 对象
     */
    public static LoginResponse fromEntity(User user, String token) {
        return LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .motto(user.getMotto())
                .build();
    }
}
