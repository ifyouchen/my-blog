package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户注册请求.
 * <p>
 * 新用户注册账号时的请求参数，需要邮箱验证码验证.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class RegisterRequest {

    /** 用户名. */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50之间")
    private String username;

    /** 注册邮箱. */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 登录密码. */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在6到64之间")
    private String password;

    /** 邮箱验证码（6位数字）. */
    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码必须为6位")
    private String emailCode;

    /** 邀请码（可选，用于邀请制注册）. */
    private String inviteCode;

    /**
     * 获取用户名.
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名.
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取注册邮箱.
     *
     * @return 邮箱地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置注册邮箱.
     *
     * @param email 邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取登录密码.
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置登录密码.
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取邮箱验证码.
     *
     * @return 验证码
     */
    public String getEmailCode() {
        return emailCode;
    }

    /**
     * 设置邮箱验证码.
     *
     * @param emailCode 验证码
     */
    public void setEmailCode(String emailCode) {
        this.emailCode = emailCode;
    }

    /**
     * 获取邀请码.
     *
     * @return 邀请码
     */
    public String getInviteCode() {
        return inviteCode;
    }

    /**
     * 设置邀请码.
     *
     * @param inviteCode 邀请码
     */
    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
