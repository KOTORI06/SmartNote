package com.smartnote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.user.*;
import com.smartnote.entity.User;
import com.smartnote.service.UserService;
import com.smartnote.util.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**@RequiredArgsConstructor
 它会为被标注的类自动生成一个构造函数
 这个构造函数会包含类中所有被 final修饰的字段
 或者带有 @NonNull注解且未在声明时初始化的字段
 */
/**
 origins：指定允许访问该接口的外部源列表
 "*"：这是一个通配符，表示允许来自任何源（任何域名、任何端口）的请求
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor//构造器注入
public class UserController {
     private final UserService userService;

     /**
      * 用户注册接口
      * POST /api/users/register
      * 新用户注册，校验必填字段和格式
      */
     @PostMapping("/register")
     public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
         log.info("用户注册请求: username={}, email={}", request.getUsername(), request.getEmail());
         User user = userService.register(request);
         return Result.success("注册成功", UserResponse.fromEntity(user));
     }
     /**
      * 用户登录接口
      * POST /api/users/login
      * 支持邮箱或手机号+密码登录，返回JWT令牌
      */
     @PostMapping("/login")
     public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
         log.info("用户登录请求: account={}", request.getAccount());
         LoginResponse loginResult = userService.login(request.getAccount(), request.getPassword());
         return Result.success("登录成功", loginResult);
     }
     /**
      * 获取当前用户信息
      * GET /api/users/me
      * 从JWT令牌中解析用户ID，返回完整的用户信息
      */

     /**@RequestAttribute
      获取拦截器/过滤器存入 request 的数据
      通过 @RequestAttribute 的参数名自动匹配
      如果参数名不一致，需显式指定
      @RequestAttribute("userId") Long uid
      */
     @GetMapping("/me")
     public Result<UserResponse> getCurrentUser(@RequestAttribute Long userId) {
         log.info("获取当前用户信息: userId={}", userId);
         User user = userService.getUserById(userId);
         return Result.success(UserResponse.fromEntity(user));
     }
     /**
      * 更新用户信息
      * PUT /api/users/me
      * 更新用户的昵称、头像、座右铭等个人信息
      */
     @PutMapping("/me")
     public Result<UserResponse> updateCurrentUser(@RequestAttribute Long userId,
                                                   @Valid @RequestBody UpdateUserRequest request) {
         log.info("更新用户信息: userId={}, request={}", userId, request);
         User updatedUser = userService.updateUser(userId, request);
         return Result.success("更新成功", UserResponse.fromEntity(updatedUser));
     }
     /**
      * 修改密码
      * PUT /api/users/password
      * 验证旧密码后修改为新密码
      */
     @PutMapping("/password")
     public Result<String> changePassword(@RequestAttribute Long userId,
                                          @Valid @RequestBody ChangePasswordRequest request) {
         log.info("修改密码: userId={}", userId);
         userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
         return Result.success("密码修改成功");
     }
     /**
      * 搜索用户
      * GET /api/users/search
      * 根据邮箱、手机号或用户名搜索用户（用于添加好友）
      */
     @GetMapping("/search")
     public Result<Page<UserResponse>> searchUsers(@RequestParam @NotBlank(message = "搜索关键词不能为空") String keyword,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "20") Integer size) {
         log.info("搜索用户: keyword={}, page={}, size={}", keyword, page, size);
         Page<User> userPage = userService.searchUsers(keyword, page, size);

         Page<UserResponse> responsePage = new Page<>(page, size, userPage.getTotal());
         // 将 User 转换为 UserResponse,为分页容器添加数据
         responsePage.setRecords(
                 userPage.getRecords().stream()
                         .map(user -> UserResponse.fromEntity(user))//对每个 User 执行：UserResponse.fromEntity(user)
                         .toList()
         );
         return Result.success(responsePage);
     }
}
