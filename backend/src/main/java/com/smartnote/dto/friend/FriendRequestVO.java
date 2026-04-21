package com.smartnote.dto.friend;

import com.smartnote.entity.FriendRelation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友申请视图对象 VO
 *
 * 用途：展示好友申请的详细信息
 * 特点：包含申请人信息和申请详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestVO {

    private Long id;//申请记录主键

    private Long userId;//申请人ID（逻辑外键，关联user.id）

    private String username;//申请人用户名

    private String avatarUrl;//申请人头像URL

    private String applyRemark;//申请备注

    private Integer status;//申请状态：0-已发送申请，1-已通过，2-已拒绝，3-已删除

    private LocalDateTime createTime;//申请创建时间

    /**
     * 截取申请备注（默认100字符）
     *
     * @param remark 完整的申请备注
     * @return 截取后的备注，如果超过100字符则添加"..."
     */
    public static String truncateRemark(String remark) {
        if (remark == null || remark.isEmpty()) {
            return "";
        }

        if (remark.length() <= 100) {
            return remark;
        }

        return remark.substring(0, 100) + "...";
    }

    /**
     * 从FriendRelation实体对象构建FriendRequestVO
     *
     * @param relation 好友关系实体对象
     * @param username 申请人用户名
     * @param avatarUrl 申请人头像URL
     * @return 构建好的FriendRequestVO对象
     */
    public static FriendRequestVO fromEntity(FriendRelation relation, String username, String avatarUrl) {
        return FriendRequestVO.builder()
                .id(relation.getId())
                .userId(relation.getUserId())
                .username(username)
                .avatarUrl(avatarUrl)
                .applyRemark(truncateRemark(relation.getApplyRemark()))
                .status(relation.getStatus())
                .createTime(relation.getCreateTime())
                .build();
    }
}
