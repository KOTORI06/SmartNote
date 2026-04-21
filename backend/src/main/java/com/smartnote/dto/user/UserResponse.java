package com.smartnote.dto.user;

import com.smartnote.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 用户信息响应 DTO

 用途：向前端返回用户信息
 特点：已脱敏，不包含密码等敏感字段
 */
@Data
@Builder// 生成建造者模式
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;//用户 ID
    private String username;//用户名
    private String email;//邮箱（部分脱敏）
    private String phone;//手机号（部分脱敏）
    private String avatarUrl;//头像 URL
    private String motto;//座右铭
    private LocalDateTime createTime;//注册时间
    private LocalDateTime lastUpdateTime;//最后更新时间

    /**
     将实体类转换为响应 DTO

     @param user 用户实体对象
     @return 脱敏后的用户响应对象
     */
    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(maskEmail(user.getEmail()))
                .phone(maskPhone(user.getPhone()))
                .avatarUrl(user.getAvatarUrl())
                .motto(user.getMotto())
                .createTime(user.getCreateTime())
                .lastUpdateTime(user.getUpdateTime())
                .build();
    }

    /**
     邮箱脱敏处理

     @param email 原始邮箱
     @return 脱敏后的邮箱
     */
    private static String maskEmail(String email) {
        // 邮箱为空
        if (email == null || email.isEmpty()) {
            return email;
        }
        //获取邮箱的 '@' 索引位置
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }
        //获取邮箱的用户名部分
        String localPart = email.substring(0, atIndex);
        //获取邮箱的域名部分
        String domain = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return "*" + localPart.substring(1) + domain;
        }

        return localPart.charAt(0) + "***" + domain;
    }

    /**
     手机号脱敏处理

     @param phone 原始手机号
     @return 脱敏后的手机号
     */
    private static String maskPhone(String phone) {
        // 手机号为空或7位以下
        if (phone == null || phone.isEmpty() || phone.length() < 7) {
            return phone;
        }
        // 获取手机号的前 3 位和后 4 位
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
