package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 文章收藏 ID 值对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public final class ArticleFavoriteId {

    /** 收藏记录 ID 的数字值。 */
    private final Long value;

    /**
     * 创建文章收藏 ID。
     *
     * @param value 收藏 ID 原始值
     */
    public ArticleFavoriteId(Long value) {
        this.value = value;
    }

    /**
     * 获取收藏 ID 原始值。
     *
     * @return 收藏 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个收藏 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleFavoriteId that = (ArticleFavoriteId) o;
        return Objects.equals(value, that.value);
    }

    /**
     * 计算收藏 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * 返回收藏 ID 字符串表示。
     *
     * @return ID 字符串
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}