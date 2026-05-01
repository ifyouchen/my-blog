package com.myblog.application.port;

/**
 * 邮件发送端口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface EmailSender {

    /**
     * 发送注册验证码。
     *
     * @param email 收件邮箱
     * @param code 验证码
     */
    void sendRegisterCode(String email, String code);

    /**
     * 发送密码重置链接。
     *
     * @param email 收件邮箱
     * @param username 用户名
     * @param resetLink 重置链接
     */
    void sendPasswordResetLink(String email, String username, String resetLink);
}
