package com.myblog.shared.exception;

/**
 * 业务异常基类。
 *
 * @author Codex
 * @since 1.0.0
 */
public abstract class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 创建业务异常。
     *
     * @param code 错误码
     * @param message 错误信息
     */
    protected BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}
