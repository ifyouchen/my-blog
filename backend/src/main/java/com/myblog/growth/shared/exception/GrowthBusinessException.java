package com.myblog.growth.shared.exception;

/**
 * 成长模块业务异常.
 * <p>
 * 表示可预期的业务规则违反，由全局异常处理器捕获并转换为 HTTP 4xx 响应。
 * 不需要打印堆栈，使用轻量级构造（不填充 stackTrace）。
 * </p>
 */
public class GrowthBusinessException extends RuntimeException {

    /** 成长模块错误码. */
    private final GrowthErrorCode errorCode;

    /**
     * 使用枚举错误码构造，消息取枚举默认消息.
     *
     * @param errorCode 错误码枚举
     */
    public GrowthBusinessException(GrowthErrorCode errorCode) {
        super(errorCode.getMessage(), null, true, false);
        this.errorCode = errorCode;
    }

    /**
     * 使用枚举错误码和自定义消息构造.
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public GrowthBusinessException(GrowthErrorCode errorCode, String message) {
        super(message, null, true, false);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码枚举.
     *
     * @return 成长模块错误码
     */
    public GrowthErrorCode getErrorCode() {
        return errorCode;
    }
}

