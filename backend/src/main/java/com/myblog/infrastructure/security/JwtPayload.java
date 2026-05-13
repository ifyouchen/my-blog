package com.myblog.infrastructure.security;

/**
 * JWT 载荷。
 *
 * @author Codex
 * @since 1.0.0
 */
public class JwtPayload {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 过期时间戳（毫秒）
     */
    private long expireAt;

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
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
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取用户角色。
     *
     * @return 用户角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置用户角色。
     *
     * @param role 用户角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取过期时间戳。
     *
     * @return 过期时间戳
     */
    public long getExpireAt() {
        return expireAt;
    }

    /**
     * 设置过期时间戳。
     *
     * @param expireAt 过期时间戳
     */
    public void setExpireAt(long expireAt) {
        this.expireAt = expireAt;
    }
}
