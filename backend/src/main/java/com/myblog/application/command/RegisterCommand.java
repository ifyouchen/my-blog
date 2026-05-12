package com.myblog.application.command;

/**
 * 注册命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class RegisterCommand {

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 明文密码
     */
    private String password;

    /**
     * 邀请码（可为 null）
     */
    private String inviteCode;

    /**
     * 邮箱验证码（可为 null）
     */
    private String emailCode;

    /**
     * 创建注册命令。
     *
     * @param username 用户名
     * @param email    邮箱
     * @param password 密码
     */
    public RegisterCommand(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    /**
     * 创建注册命令（含邀请码）。
     *
     * @param username   用户名
     * @param email      邮箱
     * @param password   密码
     * @param inviteCode 邀请码
     */
    public RegisterCommand(String username, String email, String password, String inviteCode) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.inviteCode = inviteCode;
    }

    /**
     * 创建注册命令（含邀请码和邮箱验证码）。
     *
     * @param username   用户名
     * @param email      邮箱
     * @param password   密码
     * @param inviteCode 邀请码
     * @param emailCode  邮箱验证码
     */
    public RegisterCommand(String username, String email, String password, String inviteCode, String emailCode) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.inviteCode = inviteCode;
        this.emailCode = emailCode;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取邮箱。
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 获取明文密码。
     *
     * @return 明文密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 获取邀请码。
     *
     * @return 邀请码
     */
    public String getInviteCode() {
        return inviteCode;
    }

    /**
     * 获取邮箱验证码。
     *
     * @return 邮箱验证码
     */
    public String getEmailCode() {
        return emailCode;
    }
}
