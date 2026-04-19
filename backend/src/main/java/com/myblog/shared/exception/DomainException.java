package com.myblog.shared.exception;

/**
 * 领域异常。
 *
 * @author Codex
 * @since 1.0.0
 */
public class DomainException extends BusinessException {

    /**
     * 创建领域异常。
     *
     * @param code 错误码
     * @param message 错误信息
     */
    public DomainException(int code, String message) {
        super(code, message);
    }
}
