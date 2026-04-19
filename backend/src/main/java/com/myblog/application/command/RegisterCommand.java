package com.myblog.application.command;

/**
 * 注册命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class RegisterCommand {

    private String username;
    private String email;
    private String password;

    /**
     * 创建注册命令。
     *
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     */
    public RegisterCommand(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
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
}
