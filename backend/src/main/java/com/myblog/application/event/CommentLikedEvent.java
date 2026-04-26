package com.myblog.application.event;

import java.time.LocalDateTime;

public class CommentLikedEvent {
    private final Long commentId;
    private final Long userId;
    private final LocalDateTime occurredOn;

    public CommentLikedEvent(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getCommentId() { return commentId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
}