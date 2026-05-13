package com.myblog.domain.model.valueobject;

import java.util.Objects;

/**
 * 评论点赞 ID。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class CommentLikeId {

    /** 评论点赞 ID 的数字值。 */
    private final Long value;

    /**
     * 创建评论点赞 ID。
     *
     * @param value 评论点赞 ID 原始值
     */
    public CommentLikeId(Long value) {
        this.value = value;
    }

    /**
     * 获取评论点赞 ID 原始值。
     *
     * @return 评论点赞 ID 原始值
     */
    public Long getValue() {
        return value;
    }

    /**
     * 判断两个评论点赞 ID 是否相等。
     *
     * @param o 待比较对象
     * @return 若相等则返回 {@code true}
     */
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

    /**
     * 计算评论点赞 ID 哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
