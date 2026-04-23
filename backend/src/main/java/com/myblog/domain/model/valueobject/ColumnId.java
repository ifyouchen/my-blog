package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 专栏 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ColumnId {

    private final Long value;

    public ColumnId(Long value) {
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
        if (!(o instanceof ColumnId)) {
            return false;
        }
        ColumnId columnId = (ColumnId) o;
        return Objects.equals(value, columnId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
