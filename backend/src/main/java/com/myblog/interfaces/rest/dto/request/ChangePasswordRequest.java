package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改密码请求.
 * <p>
 * 已登录用户修改自身密码的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ChangePasswordRequest {

    /** 当前密码. */
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    /** 新密码. */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度必须在 6 到 100 之间")
    private String newPassword;

    /**
     * 获取当前密码.
     *
     * @return 当前密码
     */
    public String getCurrentPassword() {
        return currentPassword;
    }

    /**
     * 设置当前密码.
     *
     * @param currentPassword 当前密码
     */
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
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

