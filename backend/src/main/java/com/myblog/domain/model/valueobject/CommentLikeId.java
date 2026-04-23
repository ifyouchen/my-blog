package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 评论点赞 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class CommentLikeId {

    private final Long value;

    public CommentLikeId(Long value) {
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
        if (!(o instanceof CommentLikeId)) {
            return false;
        }
        CommentLikeId that = (CommentLikeId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
