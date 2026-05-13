package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 重置密码请求.
 * <p>
 * 用户通过邮件验证链接重置密码的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ResetPasswordRequest {

    /** 密码重置Token（从邮件链接中获取）. */
    @NotBlank(message = "重置 Token 不能为空")
    private String token;

    /** 新密码. */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度必须在 6 到 100 之间")
    private String newPassword;

    /**
     * 获取重置Token.
     *
     * @return 重置Token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置重置Token.
     *
     * @param token 重置Token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取新密码.
     *
     * @return 新密码
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * 设置新密码.
     *
     * @param newPassword 新密码
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

