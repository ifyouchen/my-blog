package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 评论 ID 值对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public final class CommentId {

    /** 评论 ID 的数字值。 */
    private final Long value;

    /**
     * 创建评论 ID。
     *
     * @param value 评论 ID 原始值
     */
    public CommentId(Long value) {
        this.value = value;
    }

    /**
     * 获取评论 ID 原始值。
     *
     * @return 评论 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个评论 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentId commentId = (CommentId) o;
        return Objects.equals(value, commentId.value);
    }

    /**
     * 计算评论 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * 返回评论 ID 字符串表示。
     *
     * @return ID 字符串
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}