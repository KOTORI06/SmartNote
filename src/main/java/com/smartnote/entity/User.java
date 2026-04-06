package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//用户信息表实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;//用户唯一主键
    private String username;//用户名,用于登录和显示
    private String email;//邮箱，用于登录或找回密码
    private String phone;//手机号，用于登录
    private String passwordHash;//加密后的密码
    private String avatarUrl;//头像图片URL地址
    private String motto;//座右铭或个人简介
    private String extendField;//预留扩展字段（JSON格式）
    private Boolean isDeleted;//逻辑删除标志：0-未删除，1-已删除
    private LocalDateTime createTime;//记录创建时间
    private LocalDateTime updateTime;//记录最后更新时间
}
