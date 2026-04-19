package com.myblog.shared.exception;

/**
 * 通用错误码。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ErrorCode {

    public static final int PARAM_ERROR = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int SYSTEM_ERROR = 500;

    private ErrorCode() {
    }
}
