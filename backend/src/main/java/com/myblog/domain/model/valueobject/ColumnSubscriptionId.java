package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 专栏订阅 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ColumnSubscriptionId {

    private final Long value;

    public ColumnSubscriptionId(Long value) {
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
        if (!(o instanceof ColumnSubscriptionId)) {
            return false;
        }
        ColumnSubscriptionId that = (ColumnSubscriptionId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
