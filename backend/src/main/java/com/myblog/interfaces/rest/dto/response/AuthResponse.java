package com.myblog.interfaces.rest.dto.response;

/**
 * 认证响应。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthResponse {

    private String token;
    private UserResponse user;

    /**
     * 创建认证响应。
     *
     * @param token JWT 访问令牌
     * @param user 当前用户
     */
    public AuthResponse(String token, UserResponse user) {
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
    public UserResponse getUser() {
        return user;
    }
}
