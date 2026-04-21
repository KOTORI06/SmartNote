package com.smartnote.dto.user;

import com.smartnote.constant.RegexConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

//用户登录请求 DTO

@Data
public class LoginRequest {

    //登录账号,可以是邮箱或手机号
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = RegexConstant.EMAIL_OR_PHONE, message = RegexConstant.EMAIL_OR_PHONE_MESSAGE)
    private String account;

    //登录密码,明文密码，后端会与数据库密文对比
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = RegexConstant.PASSWORD, message = RegexConstant.PASSWORD_MESSAGE)
    private String password;
}
