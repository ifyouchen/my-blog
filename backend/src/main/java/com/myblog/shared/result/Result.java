package com.myblog.shared.result;

/**
 * 统一接口响应。
 *
 * @param <T> 响应数据类型
 * @author Codex
 * @since 1.0.0
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;

    /**
     * 创建空响应对象。
     */
    public Result() {
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(0, "success", data);
    }

    /**
     * 构建无数据成功响应。
     *
     * @return 成功响应
     */
    public static Result<Void> success() {
        return new Result<Void>(0, "success", null);
    }

    /**
     * 构建失败响应。
     *
     * @param code 错误码
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<T>(code, message, null);
    }

    /**
     * 获取响应码。
     *
     * @return 响应码
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置响应码。
     *
     * @param code 响应码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取响应信息。
     *
     * @return 响应信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置响应信息。
     *
     * @param message 响应信息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取响应数据。
     *
     * @return 响应数据
     */
    public T getData() {
        return data;
    }

    /**
     * 设置响应数据。
     *
     * @param data 响应数据
     */
    public void setData(T data) {
        this.data = data;
    }
}
