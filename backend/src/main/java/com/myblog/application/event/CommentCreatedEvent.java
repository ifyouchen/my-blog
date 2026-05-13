package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 评论创建事件.
 * <p>
 * 当用户新建评论时触发，用于更新文章评论数、通知文章作者等.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CommentCreatedEvent {

    /** 评论ID. */
    private final Long commentId;

    /** 所属文章ID. */
    private final Long articleId;

    /** 评论者用户ID. */
    private final Long authorId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造评论创建事件.
     *
     * @param commentId 评论ID
     * @param articleId 所属文章ID
     * @param authorId  评论者用户ID
     */
    public CommentCreatedEvent(Long commentId, Long articleId, Long authorId) {
        this.commentId = commentId;
        this.articleId = articleId;
        this.authorId = authorId;
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * 获取评论ID.
     *
     * @return 评论ID
     */
    public Long getCommentId() {
        return commentId;
    }

    /**
     * 获取所属文章ID.
     *
     * @return 文章ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 获取评论者用户ID.
     *
     * @return 评论者用户ID
     */
    public Long getAuthorId() {
        return authorId;
    }

    /**
     * 获取事件发生时间.
     *
     * @return 事件发生时间
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}