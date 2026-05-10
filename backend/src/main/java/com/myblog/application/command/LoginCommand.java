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
    private String loginIp;

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
     * 创建登录命令。
     *
     * @param account 账号
     * @param password 密码
     * @param loginIp 登录 IP
     */
    public LoginCommand(String account, String password, String loginIp) {
        this(account, password);
        this.loginIp = loginIp;
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

    /**
     * 获取登录 IP。
     *
     * @return 登录 IP
     */
    public String getLoginIp() {
        return loginIp;
    }

    /**
     * 设置登录 IP。
     *
     * @param loginIp 登录 IP
     */
    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }
}
