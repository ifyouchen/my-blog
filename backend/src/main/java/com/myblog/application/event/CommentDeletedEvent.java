package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 评论删除事件.
 * <p>
 * 当评论被删除时触发，用于级联更新子评论计数、文章评论数等.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CommentDeletedEvent {

    /** 被删除的评论ID. */
    private final Long commentId;

    /** 所属文章ID. */
    private final Long articleId;

    /** 需要递减的回复数量（含子评论）. */
    private final int decrement;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造评论删除事件.
     *
     * @param commentId 被删除的评论ID
     * @param articleId 所属文章ID
     * @param decrement 需要递减的回复数量（含子评论）
     */
    public CommentDeletedEvent(Long commentId, Long articleId, int decrement) {
        this.commentId = commentId;
        this.articleId = articleId;
        this.decrement = decrement;
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * 获取被删除的评论ID.
     *
     * @return 评论ID
     */
    public Long getCommentId() { return commentId; }

    /**
     * 获取所属文章ID.
     *
     * @return 文章ID
     */
    public Long getArticleId() { return articleId; }

    /**
     * 获取需要递减的回复数量.
     *
     * @return 回复数量
     */
    public int getDecrement() { return decrement; }

    /**
     * 获取事件发生时间.
     *
     * @return 事件发生时间
     */
    public LocalDateTime getOccurredOn() { return occurredOn; }
}