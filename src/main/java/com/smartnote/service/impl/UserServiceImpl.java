package com.smartnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.user.LoginResponse;
import com.smartnote.dto.user.RegisterRequest;
import com.smartnote.dto.user.UpdateUserRequest;
import com.smartnote.dto.user.UserResponse;
import com.smartnote.entity.User;
import com.smartnote.exception.BusinessException;
import com.smartnote.mapper.UserMapper;
import com.smartnote.service.UserService;
import com.smartnote.util.AliyunOSSOperator;
import com.smartnote.util.JwtUtil;
import com.smartnote.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor//构造器注入
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AliyunOSSOperator aliyunOSSOperator;

    /**@Transactional(rollbackFor = Exception.class)
     * 默认只回滚 RuntimeException 和 Error，加上此参数后扩展为所有异常
     */
    //注册
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest request) {
        log.info("用户注册: username={}, email={}", request.getUsername(), request.getEmail());

        //检查用户是否已存在
        if(userMapper.selectByUsername(request.getUsername()) != null){
            log.info("用户已存在，用户名：{}", request.getUsername());
            throw new BusinessException("用户已存在");
        }
        //创建用户
        User user = new User();
        //加密密码
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUsername(request.getUsername());
        user.setPasswordHash(PasswordUtil.encrypt(request.getPassword()));
        //调用mapper方法，添加用户信息到数据库
        userMapper.insert(user);
        return user;
    }

    //登录
    @Override
    public LoginResponse login(String account, String password) {
        log.info("用户登录: account={}", account);

        //设置查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(); //Lambda方式 字段名错误编译时报错
        wrapper.eq(User::getEmail, account)
                .or()
                .eq(User::getPhone, account);
        //查询对应用户
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        return LoginResponse.fromEntity(user,token);
    }

    //获取用户信息
    @Override
    public User getUserById(Long userId) {
        log.info("获取用户信息: userId={}", userId);

        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return user;
    }

    //更新用户信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(Long userId, UpdateUserRequest request) {
        log.info("更新用户信息: userId={}", userId);

        //获取对应用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            //不和原来相等
            if (!user.getUsername().equals(request.getUsername())) {
                //没有被占用
                if (userMapper.selectByUsername(request.getUsername()) != null) {
                    throw new BusinessException("用户名已存在");
                }
                user.setUsername(request.getUsername());
            }
        }

        if (request.getMotto() != null) {
            //个签
            user.setMotto(request.getMotto());
        }

        //更新时间
        user.setUpdateTime(LocalDateTime.now());
        //调用mapper，将数据更新到数据库
        userMapper.updateById(user);

        return user;
    }

    //上传头像
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        log.info("上传头像: userId={}, fileName={}", userId, file.getOriginalFilename());

        //文件不为空
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        //文件大小检查
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过5MB");
        }

        //文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只支持图片文件");
        }

        try {
            //上传文件，返回url
            String url = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());

            //找对应用户
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            user.setAvatarUrl(url);
            user.setUpdateTime(LocalDateTime.now());
            //更新数据库
            userMapper.updateById(user);

            return url;
        } catch (Exception e) {
            log.info("头像上传失败: userId={}", userId, e);
            throw new BusinessException("头像上传失败");
        }
    }

    //修改密码
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码: userId={}", userId);

        //获取对应用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!PasswordUtil.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }

        if (oldPassword.equals(newPassword)) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        //加密密码
        user.setPasswordHash(PasswordUtil.encrypt(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        //更新数据库信息
        userMapper.updateById(user);
    }

    //搜索用户
    @Override
    public Page<User> searchUsers(String keyword, Integer page, Integer size) {
        log.info("搜索用户: keyword={}", keyword);

        //创建分页对象
        Page<User> userPage = new Page<>(page, size);
        //编辑查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        //前后都模糊关键字
        wrapper.like(User::getUsername, keyword)
                .or()
                .like(User::getEmail, keyword)
                .or()
                .like(User::getPhone, keyword)
                .orderByDesc(User::getCreateTime);

        return userMapper.selectPage(userPage, wrapper);
    }
}
