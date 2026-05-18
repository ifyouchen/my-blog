package com.myblog.interfaces.rest.controller;

import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.shared.exception.BusinessException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        LOGGER.warn("业务异常，code={}，message={}", exception.getCode(), exception.getMessage());
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理成长模块业务异常。
     *
     * @param exception 成长模块业务异常
     * @return 失败响应
     */
    @ExceptionHandler(GrowthBusinessException.class)
    public Result<Void> handleGrowthBusinessException(GrowthBusinessException exception) {
        LOGGER.warn("成长模块业务异常，code={}，message={}",
                exception.getErrorCode().getCode(), exception.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR, exception.getMessage());
    }

    /**
     * 处理参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ":" + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        LOGGER.warn("参数校验失败，message={}", message);
        return Result.fail(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理 SSE 等异步请求超时（连接正常断开，无需响应体）。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException exception) {
        // 不需要返回响应体，连接已超时断开
    }

    /**
     * 处理系统异常。
     *
     * @param exception 系统异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        LOGGER.error("系统异常", exception);
        return Result.fail(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }
}
