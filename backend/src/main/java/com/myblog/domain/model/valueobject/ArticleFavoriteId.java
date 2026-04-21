package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 文章收藏 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ArticleFavoriteId {

    private final Long value;

    public ArticleFavoriteId(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleFavoriteId that = (ArticleFavoriteId) o;
        return Objects.equals(value, that.value);
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