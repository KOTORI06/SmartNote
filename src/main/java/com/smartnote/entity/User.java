package com.smartnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartnote.constant.RegexConstant;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**常用校验注解：
@NotNull - 不能为 null
@NotBlank - 字符串不能为空
@NotEmpty - 集合/数组不能为空
@Size(min=, max=) - 长度范围
@Min() / @Max() - 数值范围
@Email - 邮箱格式
@Pattern(regexp="") - 正则表达式
 **/
//用户信息表实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;//用户唯一主键

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
    private String passwordHash;//加密后的密码
    private String avatarUrl;//头像图片URL地址
    private String motto;//座右铭或个人简介
    private LocalDateTime createTime;//记录创建时间
    private LocalDateTime updateTime;//记录最后更新时间
}
