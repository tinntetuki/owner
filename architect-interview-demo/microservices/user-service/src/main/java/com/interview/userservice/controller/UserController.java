package com.interview.userservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.result.Result;
import com.interview.userservice.dto.UserDTO;
import com.interview.userservice.service.UserService;
import com.interview.userservice.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户控制器
 * 
 * @author interview
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserDTO userDTO) {
        log.info("创建用户请求: {}", userDTO.getUsername());
        UserVO userVO = userService.createUser(userDTO);
        return Result.success("创建用户成功", userVO);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable @NotNull Long id) {
        log.info("查询用户请求: id={}", id);
        UserVO userVO = userService.getUserById(id);
        return Result.success(userVO);
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/username/{username}")
    public Result<UserVO> getUserByUsername(@PathVariable @NotEmpty String username) {
        log.info("根据用户名查询用户请求: username={}", username);
        UserVO userVO = userService.getUserByUsername(username);
        return Result.success(userVO);
    }

    /**
     * 根据邮箱查询用户
     */
    @GetMapping("/email/{email}")
    public Result<UserVO> getUserByEmail(@PathVariable @NotEmpty String email) {
        log.info("根据邮箱查询用户请求: email={}", email);
        UserVO userVO = userService.getUserByEmail(email);
        return Result.success(userVO);
    }

    /**
     * 根据手机号查询用户
     */
    @GetMapping("/phone/{phone}")
    public Result<UserVO> getUserByPhone(@PathVariable @NotEmpty String phone) {
        log.info("根据手机号查询用户请求: phone={}", phone);
        UserVO userVO = userService.getUserByPhone(phone);
        return Result.success(userVO);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable @NotNull Long id, 
                                   @Valid @RequestBody UserDTO userDTO) {
        log.info("更新用户请求: id={}", id);
        UserVO userVO = userService.updateUser(id, userDTO);
        return Result.success("更新用户成功", userVO);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable @NotNull Long id) {
        log.info("删除用户请求: id={}", id);
        userService.deleteUser(id);
        return Result.success("删除用户成功");
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteUsers(@RequestBody @NotEmpty List<Long> ids) {
        log.info("批量删除用户请求: ids={}", ids);
        userService.batchDeleteUsers(ids);
        return Result.success("批量删除用户成功");
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    public Result<Page<UserVO>> getUserPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer userType) {
        log.info("分页查询用户请求: current={}, size={}, keyword={}, status={}, userType={}", 
                current, size, keyword, status, userType);
        Page<UserVO> page = userService.getUserPage(current, size, keyword, status, userType);
        return Result.success(page);
    }

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public Result<List<UserVO>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer userType) {
        log.info("查询用户列表请求: keyword={}, status={}, userType={}", keyword, status, userType);
        List<UserVO> list = userService.getUserList(keyword, status, userType);
        return Result.success(list);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable @NotNull Long id, 
                                       @RequestParam @NotNull Integer status) {
        log.info("更新用户状态请求: id={}, status={}", id, status);
        userService.updateUserStatus(id, status);
        return Result.success("更新用户状态成功");
    }

    /**
     * 更新用户类型
     */
    @PutMapping("/{id}/user-type")
    public Result<Void> updateUserType(@PathVariable @NotNull Long id, 
                                     @RequestParam @NotNull Integer userType) {
        log.info("更新用户类型请求: id={}, userType={}", id, userType);
        userService.updateUserType(id, userType);
        return Result.success("更新用户类型成功");
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check-username/{username}")
    public Result<Boolean> checkUsername(@PathVariable @NotEmpty String username) {
        log.info("检查用户名是否存在请求: username={}", username);
        boolean exists = userService.isUsernameExists(username);
        return Result.success(exists);
    }

    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/check-email/{email}")
    public Result<Boolean> checkEmail(@PathVariable @NotEmpty String email) {
        log.info("检查邮箱是否存在请求: email={}", email);
        boolean exists = userService.isEmailExists(email);
        return Result.success(exists);
    }

    /**
     * 检查手机号是否存在
     */
    @GetMapping("/check-phone/{phone}")
    public Result<Boolean> checkPhone(@PathVariable @NotEmpty String phone) {
        log.info("检查手机号是否存在请求: phone={}", phone);
        boolean exists = userService.isPhoneExists(phone);
        return Result.success(exists);
    }

    /**
     * 统计用户数量
     */
    @GetMapping("/count")
    public Result<Long> countUsers() {
        log.info("统计用户数量请求");
        Long count = userService.countUsers();
        return Result.success(count);
    }

    /**
     * 统计指定状态的用户数量
     */
    @GetMapping("/count/status/{status}")
    public Result<Long> countUsersByStatus(@PathVariable @NotNull Integer status) {
        log.info("统计指定状态的用户数量请求: status={}", status);
        Long count = userService.countUsersByStatus(status);
        return Result.success(count);
    }

    /**
     * 统计指定类型的用户数量
     */
    @GetMapping("/count/user-type/{userType}")
    public Result<Long> countUsersByUserType(@PathVariable @NotNull Integer userType) {
        log.info("统计指定类型的用户数量请求: userType={}", userType);
        Long count = userService.countUsersByUserType(userType);
        return Result.success(count);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserVO> login(@RequestParam @NotEmpty String username,
                              @RequestParam @NotEmpty String password,
                              HttpServletRequest request) {
        log.info("用户登录请求: username={}", username);
        String loginIp = getClientIp(request);
        UserVO userVO = userService.login(username, password, loginIp);
        return Result.success("登录成功", userVO);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody UserDTO userDTO) {
        log.info("用户注册请求: username={}", userDTO.getUsername());
        UserVO userVO = userService.register(userDTO);
        return Result.success("注册成功", userVO);
    }

    /**
     * 修改密码
     */
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(@PathVariable @NotNull Long id,
                                     @RequestParam @NotEmpty String oldPassword,
                                     @RequestParam @NotEmpty String newPassword) {
        log.info("修改密码请求: id={}", id);
        userService.changePassword(id, oldPassword, newPassword);
        return Result.success("修改密码成功");
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable @NotNull Long id,
                                    @RequestParam @NotEmpty String newPassword) {
        log.info("重置密码请求: id={}", id);
        userService.resetPassword(id, newPassword);
        return Result.success("重置密码成功");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
