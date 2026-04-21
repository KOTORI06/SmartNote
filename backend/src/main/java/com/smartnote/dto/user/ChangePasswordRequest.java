package com.smartnote.dto.user;

import com.smartnote.constant.RegexConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码请求 DTO
 *
 * 用途：接收用户修改密码请求
 * 特点：需要验证旧密码，新密码需符合强度要求
 */
@Data
public class ChangePasswordRequest {

    /**
     旧密码
     - 用于身份验证
     - 必须与当前密码匹配
     */
    @NotBlank(message = "旧密码不能为空")
    @Pattern(regexp = RegexConstant.PASSWORD, message = RegexConstant.PASSWORD_MESSAGE)
    private String oldPassword;

    /**
     新密码
     - 长度 8-20 位，必须包含大小写字母和数字
     - 不能与旧密码相同
     */
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = RegexConstant.PASSWORD, message = RegexConstant.PASSWORD_MESSAGE)
    private String newPassword;
}
