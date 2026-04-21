package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartnote.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);
}
