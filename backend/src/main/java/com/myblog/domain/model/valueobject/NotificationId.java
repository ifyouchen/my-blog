package com.myblog.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 通知 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class NotificationId implements Serializable {

    /** 通知 ID 的数字值，必须大于 0。 */
    private final Long value;

    /**
     * 创建通知 ID。
     *
     * @param value 通知 ID 原始值
     * @throws IllegalArgumentException 若 value 为 null 或小于等于 0
     */
    public NotificationId(Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("通知ID不能为空");
        }
        this.value = value;
    }

    /**
     * 获取通知 ID 原始值。
     *
     * @return 通知 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个通知 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationId)) {
            return false;
        }
        NotificationId that = (NotificationId) o;
        return Objects.equals(value, that.value);
    }

    /**
     * 计算通知 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}