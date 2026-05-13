package com.myblog.infrastructure.security;

import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;

/**
 * 当前请求认证上下文。
 *
 * <p>基于 ThreadLocal 保存单次请求内的登录信息，必须在请求结束时调用
 * {@link #clear()} 释放，避免线程复用时把上一个请求的身份残留到下一个请求。</p>
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
    public static Long getCurrentUserId() {
        JwtPayload payload = HOLDER.get();
        if (payload == null) {
            return null;
        }
        return payload.getUserId();
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出异常。
     *
     * @return 当前用户 ID
     * @throws ApplicationException 未登录时
     */
    public static Long getRequiredUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return userId;
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
