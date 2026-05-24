package com.myblog.shared.exception;

/**
 * 通用错误码。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ErrorCode {

    /**
     * 请求参数错误（400）
     */
    public static final int PARAM_ERROR = 400;

    /**
     * 未认证，需要登录（401）
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 无权限访问（403）
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在（404）
     */
    public static final int NOT_FOUND = 404;

    /**
     * 资源冲突（409）
     */
    public static final int CONFLICT = 409;

    /**
     * 请求过频，触发限流（429）
     */
    public static final int TOO_MANY_REQUESTS = 429;

    /**
     * 系统内部错误（500）
     */
    public static final int SYSTEM_ERROR = 500;

    private ErrorCode() {
    }
}
