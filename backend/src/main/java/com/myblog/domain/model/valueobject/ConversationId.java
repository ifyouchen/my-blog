package com.myblog.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 会话 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ConversationId implements Serializable {

    private final Long value;

    public ConversationId(Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("会话ID不能为空");
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
        if (!(o instanceof ConversationId)) {
            return false;
        }
        ConversationId that = (ConversationId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
