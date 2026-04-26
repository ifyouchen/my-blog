package com.myblog.application.event;

import java.time.LocalDateTime;

public class CommentDeletedEvent {
    private final Long commentId;
    private final Long articleId;
    private final int decrement;
    private final LocalDateTime occurredOn;

    public CommentDeletedEvent(Long commentId, Long articleId, int decrement) {
        this.commentId = commentId;
        this.articleId = articleId;
        this.decrement = decrement;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getCommentId() { return commentId; }
    public Long getArticleId() { return articleId; }
    public int getDecrement() { return decrement; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
}