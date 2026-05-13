package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 忘记密码请求.
 * <p>
 * 用户通过邮箱发起密码重置流程的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ForgotPasswordRequest {

    /** 用户注册邮箱. */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 获取用户邮箱.
     *
     * @return 邮箱地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置用户邮箱.
     *
     * @param email 邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }
}

