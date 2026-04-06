package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//好友关系表实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("friend_relation")
public class FriendRelation {

    @TableId(type = IdType.AUTO)
    private Long id;//关系记录主键
    private Long userId;//主动方用户ID（逻辑外键，关联user.id）
    private Long friendId;//好友用户ID（逻辑外键，关联user.id）
    private String groupName;//好友分组名称
    private Integer status;//关系状态：0-已发送申请，1-已是好友，2-已拒绝，3-已删除
    private String applyRemark;//好友申请备注
    private LocalDateTime createTime;//记录创建时间
    private LocalDateTime updateTime;//状态更新时间
}
