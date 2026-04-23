package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.CommentLikeId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 评论点赞聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CommentLike {

    private CommentLikeId id;
    private CommentId commentId;
    private UserId userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private CommentLike() {
    }

    public static CommentLike create(Long id, CommentId commentId, UserId userId) {
        CommentLike like = new CommentLike();
        like.id = new CommentLikeId(id);
        like.commentId = commentId;
        like.userId = userId;
        like.createdAt = LocalDateTime.now();
        like.updatedAt = like.createdAt;
        like.deletedAt = null;
        like.version = 0;
        return like;
    }

    public static CommentLike restore(Long id, CommentId commentId, UserId userId,
                                      LocalDateTime createdAt, LocalDateTime updatedAt,
                                      LocalDateTime deletedAt, Integer version) {
        CommentLike like = new CommentLike();
        like.id = new CommentLikeId(id);
        like.commentId = commentId;
        like.userId = userId;
        like.createdAt = createdAt;
        like.updatedAt = updatedAt;
        like.deletedAt = deletedAt;
        like.version = version;
        return like;
    }

    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public CommentLikeId getId() {
        return id;
    }

    public CommentId getCommentId() {
        return commentId;
    }

    public UserId getUserId() {
        return userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
