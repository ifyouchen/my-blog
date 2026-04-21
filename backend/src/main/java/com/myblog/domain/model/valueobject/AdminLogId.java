package com.myblog.domain.model.valueobject;

/**
 * 管理员操作日志 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminLogId {

    private final Long value;

    /**
     * 创建管理员操作日志 ID。
     *
     * @param value 日志 ID
     */
    public AdminLogId(Long value) {
        this.value = value;
    }

    /**
     * 获取日志 ID 值。
     *
     * @return 日志 ID 值
     */
    public Long getValue() {
        return value;
    }
}
