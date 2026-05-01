package com.myblog.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 消息 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class MessageId implements Serializable {

    private final Long value;

    public MessageId(Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("消息ID不能为空");
        }
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
