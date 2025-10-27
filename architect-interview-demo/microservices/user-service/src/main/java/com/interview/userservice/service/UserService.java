package com.interview.userservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.userservice.dto.UserDTO;
import com.interview.userservice.entity.User;
import com.interview.userservice.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 * 
 * @author interview
 * @since 2024-01-01
 */
public interface UserService {

    /**
     * 创建用户
     */
    UserVO createUser(UserDTO userDTO);

    /**
     * 根据ID查询用户
     */
    UserVO getUserById(Long id);

    /**
     * 根据用户名查询用户
     */
    UserVO getUserByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    UserVO getUserByEmail(String email);

    /**
     * 根据手机号查询用户
     */
    UserVO getUserByPhone(String phone);

    /**
     * 更新用户
     */
    UserVO updateUser(Long id, UserDTO userDTO);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 批量删除用户
     */
    void batchDeleteUsers(List<Long> ids);

    /**
     * 分页查询用户
     */
    Page<UserVO> getUserPage(int current, int size, String keyword, Integer status, Integer userType);

    /**
     * 查询用户列表
     */
    List<UserVO> getUserList(String keyword, Integer status, Integer userType);

    /**
     * 更新用户状态
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 更新用户类型
     */
    void updateUserType(Long id, Integer userType);

    /**
     * 更新最后登录信息
     */
    void updateLastLoginInfo(Long id, String loginIp);

    /**
     * 检查用户名是否存在
     */
    boolean isUsernameExists(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean isEmailExists(String email);

    /**
     * 检查手机号是否存在
     */
    boolean isPhoneExists(String phone);

    /**
     * 统计用户数量
     */
    Long countUsers();

    /**
     * 统计指定状态的用户数量
     */
    Long countUsersByStatus(Integer status);

    /**
     * 统计指定类型的用户数量
     */
    Long countUsersByUserType(Integer userType);

    /**
     * 用户登录
     */
    UserVO login(String username, String password, String loginIp);

    /**
     * 用户注册
     */
    UserVO register(UserDTO userDTO);

    /**
     * 修改密码
     */
    void changePassword(Long id, String oldPassword, String newPassword);

    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);
}
