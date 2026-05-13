package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 评论点赞事件.
 * <p>
 * 当用户对评论进行点赞操作时触发，用于更新评论点赞数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CommentLikedEvent {

    /** 评论ID. */
    private final Long commentId;

    /** 点赞用户ID. */
    private final Long userId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造评论点赞事件.
     *
     * @param commentId 评论ID
     * @param userId    点赞用户ID
     */
    public CommentLikedEvent(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * 获取评论ID.
     *
     * @return 评论ID
     */
    public Long getCommentId() { return commentId; }

    /**
     * 获取点赞用户ID.
     *
     * @return 点赞用户ID
     */
    public Long getUserId() { return userId; }

    /**
     * 获取事件发生时间.
     *
     * @return 事件发生时间
     */
    public LocalDateTime getOccurredOn() { return occurredOn; }
}