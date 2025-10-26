package com.interview.userservice.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户VO
 * 
 * @author interview
 * @since 2024-01-01
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 性别描述
     */
    private String genderDesc;

    /**
     * 生日
     */
    private LocalDateTime birthday;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 用户状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 用户状态描述
     */
    private String statusDesc;

    /**
     * 用户类型：0-普通用户，1-VIP用户，2-管理员
     */
    private Integer userType;

    /**
     * 用户类型描述
     */
    private String userTypeDesc;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
