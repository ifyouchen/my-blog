package com.myblog.infrastructure.security;

/**
 * 当前请求认证上下文。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class AuthContext {

    private static final ThreadLocal<JwtPayload> HOLDER = new ThreadLocal<JwtPayload>();

    private AuthContext() {
    }

    /**
     * 设置当前请求认证信息。
     *
     * @param payload JWT 载荷
     */
    public static void set(JwtPayload payload) {
        HOLDER.set(payload);
    }

    /**
     * 获取当前请求认证信息。
     *
     * @return JWT 载荷
     */
    public static JwtPayload get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 当前用户 ID，未登录时返回 null
     */
    public static Long getRequiredUserId() {
        JwtPayload payload = HOLDER.get();
        if (payload == null) {
            return null;
        }
        return payload.getUserId();
    }

    /**
     * 获取当前登录用户角色。
     *
     * @return 当前用户角色，未登录时返回 null
     */
    public static String getRole() {
        JwtPayload payload = HOLDER.get();
        if (payload == null) {
            return null;
        }
        return payload.getRole();
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 当前用户名，未登录时返回 null
     */
    public static String getUsername() {
        JwtPayload payload = HOLDER.get();
        if (payload == null) {
            return null;
        }
        return payload.getUsername();
    }

    /**
     * 清理当前请求认证信息。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
