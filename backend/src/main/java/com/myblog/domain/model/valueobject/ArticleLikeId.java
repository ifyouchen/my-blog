package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 文章点赞 ID 值对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ArticleLikeId {

    /** 点赞记录 ID 的数字值。 */
    private final Long value;

    /**
     * 创建文章点赞 ID。
     *
     * @param value 点赞 ID 原始值
     */
    public ArticleLikeId(Long value) {
        this.value = value;
    }

    /**
     * 获取点赞 ID 原始值。
     *
     * @return 点赞 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个点赞 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleLikeId that = (ArticleLikeId) o;
        return Objects.equals(value, that.value);
    }

    /**
     * 计算点赞 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * 返回点赞 ID 字符串表示。
     *
     * @return ID 字符串
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}