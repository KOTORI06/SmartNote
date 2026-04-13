package com.smartnote.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//好友分组表实体类
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("friend_group")
public class Group {

    @TableId(type = IdType.AUTO)
    private Long id;//分组ID(唯一主键)
    private Long userId;//分组所属的用户ID（逻辑外键，关联user.id）
    private String groupName;//分组名称
    private LocalDateTime createTime;//分组创建时间
}
