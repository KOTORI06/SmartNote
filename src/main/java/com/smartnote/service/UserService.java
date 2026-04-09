package com.smartnote.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.constant.RegexConstant;
import com.smartnote.dto.user.LoginResponse;
import com.smartnote.dto.user.RegisterRequest;
import com.smartnote.dto.user.UpdateUserRequest;
import com.smartnote.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    // 用户注册
    User register(RegisterRequest request);

    // 用户登录
    LoginResponse login(String account, String password);

    // 获取用户信息
    User getUserById(Long userId);

    // 更新用户信息
    User updateUser(Long userId, UpdateUserRequest request);

    // 上传头像
    String uploadAvatar(Long userId, MultipartFile file);

    // 修改密码
    void changePassword(Long userId, String oldPassword, String newPassword);

    // 搜索用户
    Page<User> searchUsers(String keyword, Integer page, Integer size);
}
