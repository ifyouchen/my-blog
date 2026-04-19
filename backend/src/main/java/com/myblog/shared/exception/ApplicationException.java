package com.myblog.shared.exception;

/**
 * 应用异常。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ApplicationException extends BusinessException {

    /**
     * 创建应用异常。
     *
     * @param code 错误码
     * @param message 错误信息
     */
    public ApplicationException(int code, String message) {
        super(code, message);
    }
}
