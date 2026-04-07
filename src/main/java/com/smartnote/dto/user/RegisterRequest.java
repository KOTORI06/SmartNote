package com.smartnote.dto.user;

import com.smartnote.constant.RegexConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

//用户注册请求 DTO
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = RegexConstant.USERNAME, message = RegexConstant.USERNAME_MESSAGE)
    private String username;//用户名,用于登录和显示

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = RegexConstant.EMAIL, message = RegexConstant.EMAIL_MESSAGE)
    private String email;//邮箱，用于登录或找回密码

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegexConstant.PHONE, message = RegexConstant.PHONE_MESSAGE)
    private String phone;//手机号，用于登录

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = RegexConstant.PASSWORD, message = RegexConstant.PASSWORD_MESSAGE)
    private String password;//明文密码
}
