package com.myblog.application.event;

import java.time.LocalDateTime;

public class CommentCreatedEvent {
    private final Long commentId;
    private final Long articleId;
    private final Long authorId;
    private final LocalDateTime occurredOn;

    public CommentCreatedEvent(Long commentId, Long articleId, Long authorId) {
        this.commentId = commentId;
        this.articleId = articleId;
        this.authorId = authorId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getCommentId() {
        return commentId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}