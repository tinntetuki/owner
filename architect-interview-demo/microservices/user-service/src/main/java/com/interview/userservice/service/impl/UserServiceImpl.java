package com.interview.userservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.exception.BusinessException;
import com.interview.common.result.ResultCode;
import com.interview.userservice.dto.UserDTO;
import com.interview.userservice.entity.User;
import com.interview.userservice.mapper.UserMapper;
import com.interview.userservice.service.UserService;
import com.interview.userservice.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * @author interview
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserDTO userDTO) {
        log.info("创建用户: {}", userDTO.getUsername());

        // 检查用户名是否存在
        if (isUsernameExists(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 检查邮箱是否存在
        if (StrUtil.isNotBlank(userDTO.getEmail()) && isEmailExists(userDTO.getEmail())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "邮箱已存在");
        }

        // 检查手机号是否存在
        if (StrUtil.isNotBlank(userDTO.getPhone()) && isPhoneExists(userDTO.getPhone())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "手机号已存在");
        }

        // 创建用户实体
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setStatus(1); // 默认正常状态
        user.setUserType(0); // 默认普通用户
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "创建用户失败");
        }

        log.info("用户创建成功: {}", user.getId());
        return convertToVO(user);
    }

    @Override
    public UserVO getUserById(Long id) {
        log.info("根据ID查询用户: {}", id);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return convertToVO(user);
    }

    @Override
    public UserVO getUserByUsername(String username) {
        log.info("根据用户名查询用户: {}", username);

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return convertToVO(user);
    }

    @Override
    public UserVO getUserByEmail(String email) {
        log.info("根据邮箱查询用户: {}", email);

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return convertToVO(user);
    }

    @Override
    public UserVO getUserByPhone(String phone) {
        log.info("根据手机号查询用户: {}", phone);

        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long id, UserDTO userDTO) {
        log.info("更新用户: {}", id);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查用户名是否被其他用户使用
        if (StrUtil.isNotBlank(userDTO.getUsername()) && !user.getUsername().equals(userDTO.getUsername())) {
            if (isUsernameExists(userDTO.getUsername())) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "用户名已存在");
            }
        }

        // 检查邮箱是否被其他用户使用
        if (StrUtil.isNotBlank(userDTO.getEmail()) && !user.getEmail().equals(userDTO.getEmail())) {
            if (isEmailExists(userDTO.getEmail())) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "邮箱已存在");
            }
        }

        // 检查手机号是否被其他用户使用
        if (StrUtil.isNotBlank(userDTO.getPhone()) && !user.getPhone().equals(userDTO.getPhone())) {
            if (isPhoneExists(userDTO.getPhone())) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "手机号已存在");
            }
        }

        // 更新用户信息
        BeanUtils.copyProperties(userDTO, user, "id", "password", "createTime", "createBy");
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "更新用户失败");
        }

        log.info("用户更新成功: {}", id);
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        log.info("删除用户: {}", id);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        int result = userMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "删除用户失败");
        }

        log.info("用户删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(List<Long> ids) {
        log.info("批量删除用户: {}", ids);

        if (ids == null || ids.isEmpty()) {
            return;
        }

        int result = userMapper.deleteBatchIds(ids);
        if (result != ids.size()) {
            throw new BusinessException(ResultCode.ERROR, "批量删除用户失败");
        }

        log.info("批量删除用户成功: {}", ids);
    }

    @Override
    public Page<UserVO> getUserPage(int current, int size, String keyword, Integer status, Integer userType) {
        log.info("分页查询用户: current={}, size={}, keyword={}, status={}, userType={}", 
                current, size, keyword, status, userType);

        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword)
                    .or()
                    .like(User::getPhone, keyword)
            );
        }

        // 状态筛选
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }

        // 用户类型筛选
        if (userType != null) {
            queryWrapper.eq(User::getUserType, userType);
        }

        queryWrapper.orderByDesc(User::getCreateTime);

        Page<User> userPage = userMapper.selectPage(page, queryWrapper);
        Page<UserVO> voPage = new Page<>(current, size, userPage.getTotal());

        List<UserVO> voList = userPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<UserVO> getUserList(String keyword, Integer status, Integer userType) {
        log.info("查询用户列表: keyword={}, status={}, userType={}", keyword, status, userType);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword)
                    .or()
                    .like(User::getPhone, keyword)
            );
        }

        // 状态筛选
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }

        // 用户类型筛选
        if (userType != null) {
            queryWrapper.eq(User::getUserType, userType);
        }

        queryWrapper.orderByDesc(User::getCreateTime);

        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, Integer status) {
        log.info("更新用户状态: id={}, status={}", id, status);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "更新用户状态失败");
        }

        log.info("用户状态更新成功: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserType(Long id, Integer userType) {
        log.info("更新用户类型: id={}, userType={}", id, userType);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setUserType(userType);
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "更新用户类型失败");
        }

        log.info("用户类型更新成功: id={}, userType={}", id, userType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginInfo(Long id, String loginIp) {
        log.info("更新最后登录信息: id={}, loginIp={}", id, loginIp);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "更新最后登录信息失败");
        }

        log.info("最后登录信息更新成功: id={}, loginIp={}", id, loginIp);
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userMapper.selectByUsername(username) != null;
    }

    @Override
    public boolean isEmailExists(String email) {
        return userMapper.selectByEmail(email) != null;
    }

    @Override
    public boolean isPhoneExists(String phone) {
        return userMapper.selectByPhone(phone) != null;
    }

    @Override
    public Long countUsers() {
        return userMapper.countUsers();
    }

    @Override
    public Long countUsersByStatus(Integer status) {
        return userMapper.countUsersByStatus(status);
    }

    @Override
    public Long countUsersByUserType(Integer userType) {
        return userMapper.countUsersByUserType(userType);
    }

    @Override
    public UserVO login(String username, String password, String loginIp) {
        log.info("用户登录: username={}", username);

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_STATE_ERROR, "用户状态异常");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 更新最后登录信息
        updateLastLoginInfo(user.getId(), loginIp);

        log.info("用户登录成功: id={}, username={}", user.getId(), username);
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserDTO userDTO) {
        log.info("用户注册: username={}", userDTO.getUsername());

        // 检查用户名是否存在
        if (isUsernameExists(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 检查邮箱是否存在
        if (StrUtil.isNotBlank(userDTO.getEmail()) && isEmailExists(userDTO.getEmail())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "邮箱已存在");
        }

        // 检查手机号是否存在
        if (StrUtil.isNotBlank(userDTO.getPhone()) && isPhoneExists(userDTO.getPhone())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "手机号已存在");
        }

        // 创建用户实体
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setStatus(1); // 默认正常状态
        user.setUserType(0); // 默认普通用户
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "用户注册失败");
        }

        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String oldPassword, String newPassword) {
        log.info("修改密码: id={}", id);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "修改密码失败");
        }

        log.info("密码修改成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        log.info("重置密码: id={}", id);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "重置密码失败");
        }

        log.info("密码重置成功: id={}", id);
    }

    /**
     * 转换为VO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);

        // 设置描述信息
        vo.setGenderDesc(getGenderDesc(user.getGender()));
        vo.setStatusDesc(getStatusDesc(user.getStatus()));
        vo.setUserTypeDesc(getUserTypeDesc(user.getUserType()));

        // 计算年龄
        if (user.getBirthday() != null) {
            vo.setAge(calculateAge(user.getBirthday()));
        }

        return vo;
    }

    /**
     * 获取性别描述
     */
    private String getGenderDesc(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 1:
                return "男";
            case 2:
                return "女";
            default:
                return "未知";
        }
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "禁用";
            case 1:
                return "正常";
            default:
                return "未知";
        }
    }

    /**
     * 获取用户类型描述
     */
    private String getUserTypeDesc(Integer userType) {
        if (userType == null) {
            return "未知";
        }
        switch (userType) {
            case 0:
                return "普通用户";
            case 1:
                return "VIP用户";
            case 2:
                return "管理员";
            default:
                return "未知";
        }
    }

    /**
     * 计算年龄
     */
    private Integer calculateAge(LocalDateTime birthday) {
        if (birthday == null) {
            return null;
        }
        return LocalDateTime.now().getYear() - birthday.getYear();
    }
}
