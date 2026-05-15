package com.myblog.growth.shared.exception;

/**
 * 成长模块业务错误码枚举.
 * <p>
 * 错误码格式：{@code GROWTH_XXXX}，前缀 GROWTH 标识成长模块。
 * HTTP 状态码映射由 {@code GlobalExceptionHandler} 统一处理。
 * </p>
 */
public enum GrowthErrorCode {

    /** 参数非法（通用）. */
    PARAM_INVALID("GROWTH_0001", "参数非法"),

    /** 经验规则重复，同一 eventType + role 只能存在一条有效规则. */
    RULE_DUPLICATE("GROWTH_0002", "规则已存在"),

    /** 乐观锁版本冲突，请刷新后重试. */
    OPTIMISTIC_LOCK_CONFLICT("GROWTH_0003", "数据已被修改，请刷新后重试"),

    /** 成长账户不存在. */
    ACCOUNT_NOT_FOUND("GROWTH_0004", "成长账户不存在"),

    /** 每日经验上限已达到. */
    DAILY_LIMIT_REACHED("GROWTH_0005", "今日经验获取已达上限"),

    /** 积分不足. */
    INSUFFICIENT_POINTS("GROWTH_0006", "积分不足"),

    /** 文章已解锁，不可重复解锁. */
    ALREADY_UNLOCKED("GROWTH_0007", "文章已解锁");

    /** 错误码字符串. */
    private final String code;

    /** 默认错误消息. */
    private final String message;

    /**
     * 构造枚举.
     *
     * @param code    错误码字符串
     * @param message 默认错误消息
     */
    GrowthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码字符串.
     *
     * @return 错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取默认错误消息.
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }
}

