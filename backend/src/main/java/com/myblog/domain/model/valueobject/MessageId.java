package com.myblog.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 消息 ID 值对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class MessageId implements Serializable {

    /** 消息 ID 的数字值，必须大于 0。 */
    private final Long value;

    /**
     * 创建消息 ID。
     *
     * @param value 消息 ID 原始值
     * @throws IllegalArgumentException 若 value 为 null 或小于等于 0
     */
    public MessageId(Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("消息ID不能为空");
        }
        this.value = value;
    }

    /**
     * 获取消息 ID 原始值。
     *
     * @return 消息 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个消息 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageId)) {
            return false;
        }
        MessageId that = (MessageId) o;
        return Objects.equals(value, that.value);
    }

    /**
     * 计算消息 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
