package com.myblog.application.dto;

/**
 * 认证应用数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthDTO {

    private String token;
    private UserDTO user;

    /**
     * 创建认证应用数据。
     *
     * @param token JWT 访问令牌
     * @param user 当前用户
     */
    public AuthDTO(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    /**
     * 获取 JWT 访问令牌。
     *
     * @return JWT 访问令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 获取当前用户。
     *
     * @return 当前用户
     */
    public UserDTO getUser() {
        return user;
    }
}
