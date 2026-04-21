package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 评论 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class CommentId {

    private final Long value;

    public CommentId(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentId commentId = (CommentId) o;
        return Objects.equals(value, commentId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}