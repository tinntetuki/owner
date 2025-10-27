package com.interview.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.userservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 * 
 * @author interview
 * @since 2024-01-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM user WHERE email = #{email} AND deleted = 0")
    User selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM user WHERE phone = #{phone} AND deleted = 0")
    User selectByPhone(@Param("phone") String phone);

    /**
     * 根据用户状态查询用户列表
     */
    @Select("SELECT * FROM user WHERE status = #{status} AND deleted = 0 ORDER BY create_time DESC")
    List<User> selectByStatus(@Param("status") Integer status);

    /**
     * 根据用户类型查询用户列表
     */
    @Select("SELECT * FROM user WHERE user_type = #{userType} AND deleted = 0 ORDER BY create_time DESC")
    List<User> selectByUserType(@Param("userType") Integer userType);

    /**
     * 统计用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE deleted = 0")
    Long countUsers();

    /**
     * 统计指定状态的用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE status = #{status} AND deleted = 0")
    Long countUsersByStatus(@Param("status") Integer status);

    /**
     * 统计指定类型的用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE user_type = #{userType} AND deleted = 0")
    Long countUsersByUserType(@Param("userType") Integer userType);
}
