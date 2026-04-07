package com.smartnote.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 更新用户信息请求 DTO

 用途：接收用户个人信息修改请求
 特点：所有字段可选，只更新传入的字段
 */
@Data
public class UpdateUserRequest {

    /**
     用户名
     - 可选字段，不传则不更新
     */
    @Size(min = 4, max = 20, message = "昵称长度为 4-20 个字符")
    private String username;

    /**
     头像 URL
     - 用户上传后的头像地址
     - 可选字段，不传则不更新
     */
    @Size(max = 500, message = "头像 URL 过长")
    private String avatarUrl;

    /**
     座右铭/个人简介
     - 用户个性签名
     - 可选字段，不传则不更新
     */
    @Size(max = 200, message = "座右铭不能超过 200 个字符")
    private String motto;
}
