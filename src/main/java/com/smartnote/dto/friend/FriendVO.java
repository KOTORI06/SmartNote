package com.smartnote.dto.friend;

import com.smartnote.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友视图对象 VO
 *
 * 用途：在好友列表中展示好友的详细信息
 * 特点：包含用户基本信息和好友关系信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendVO {

    private Long id;//好友ID

    private String username;//好友用户名

    private String email;//好友邮箱

    private String phone;//好友手机号

    private String avatarUrl;//好友头像URL

    private String motto;//好友座右铭

    private String groupName;

    private LocalDateTime friendSince;//添加好友时间

    /**
     * 截取座右铭（默认50字符）
     *
     * @param motto 完整的座右铭
     * @return 截取后的座右铭，如果超过50字符则添加"..."
     */
    public static String turnMotto(String motto) {
        if (motto == null || motto.isEmpty()) {
            return "";
        }

        if (motto.length() <= 50) {
            return motto;
        }

        return motto.substring(0, 50) + "...";
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
    /**
     * 从User实体对象构建FriendVO
     *
     * @param user 用户实体对象
     * @param groupName 好友分组名称
     * @param friendSince 成为好友的时间
     * @return 构建好的FriendVO对象
     */
    public static FriendVO fromEntity(User user, String groupName, LocalDateTime friendSince) {
        return FriendVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(maskEmail(user.getEmail()))
                .phone(maskPhone(user.getPhone()))
                .avatarUrl(user.getAvatarUrl())
                .motto(turnMotto(user.getMotto()))
                .groupName(groupName)
                .friendSince(friendSince)
                .build();
    }
}
