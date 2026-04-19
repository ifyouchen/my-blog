package com.myblog.application.command;

/**
 * 登录命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class LoginCommand {

    private String account;
    private String password;

    /**
     * 创建登录命令。
     *
     * @param account 账号
     * @param password 密码
     */
    public LoginCommand(String account, String password) {
        this.account = account;
        this.password = password;
    }

    /**
     * 获取登录账号。
     *
     * @return 登录账号
     */
    public String getAccount() {
        return account;
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
