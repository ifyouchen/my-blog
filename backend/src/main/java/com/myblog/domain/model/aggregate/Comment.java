package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.exception.DomainException;
import com.myblog.shared.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * 评论聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Comment {

    private CommentId id;
    private ArticleId articleId;
    private UserId userId;
    private CommentId parentId;
    private String content;
    private CommentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Comment() {
    }

    /**
     * 创建评论聚合根。
     *
     * @param id 评论 ID
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @param parentId 父评论 ID
     * @param content 评论内容
     * @return 评论聚合根
     */
    public static Comment create(Long id, ArticleId articleId, UserId userId, Long parentId, String content) {
        if (articleId == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "文章 ID 不能为空");
        }
        if (userId == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "用户 ID 不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "评论内容不能为空");
        }
        if (content.length() > 1000) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "评论内容不能超过 1000 字符");
        }
        Comment comment = new Comment();
        comment.id = new CommentId(id);
        comment.articleId = articleId;
        comment.userId = userId;
        comment.parentId = parentId != null && parentId > 0 ? new CommentId(parentId) : null;
        comment.content = content.trim();
        comment.status = CommentStatus.PUBLISHED;
        comment.createdAt = LocalDateTime.now();
        comment.updatedAt = comment.createdAt;
        return comment;
    }

    /**
     * 从持久化数据还原评论聚合根。
     *
     * @param id 评论 ID
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @param parentId 父评论 ID
     * @param content 评论内容
     * @param status 评论状态
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @return 评论聚合根
     */
    public static Comment restore(Long id, ArticleId articleId, UserId userId, Long parentId,
                                 String content, CommentStatus status,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        Comment comment = new Comment();
        comment.id = new CommentId(id);
        comment.articleId = articleId;
        comment.userId = userId;
        comment.parentId = parentId != null && parentId > 0 ? new CommentId(parentId) : null;
        comment.content = content;
        comment.status = status;
        comment.createdAt = createdAt;
        comment.updatedAt = updatedAt;
        return comment;
    }

    /**
     * 删除评论。
     */
    public void delete() {
        if (CommentStatus.DELETED.equals(this.status)) {
            throw new DomainException(ErrorCode.CONFLICT, "评论已删除");
        }
        this.status = CommentStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断评论是否已删除。
     *
     * @return 是否已删除
     */
    public boolean isDeleted() {
        return CommentStatus.DELETED.equals(this.status);
    }

    /**
     * 获取评论 ID。
     *
     * @return 评论 ID
     */
    public CommentId getId() {
        return id;
    }

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public ArticleId getArticleId() {
        return articleId;
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * 获取父评论 ID。
     *
     * @return 父评论 ID
     */
    public Long getParentId() {
        return parentId != null ? parentId.getValue() : null;
    }

    /**
     * 获取评论内容。
     *
     * @return 评论内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取评论状态。
     *
     * @return 评论状态
     */
    public CommentStatus getStatus() {
        return status;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public enum CommentStatus {
        PUBLISHED,
        DELETED
    }
}
