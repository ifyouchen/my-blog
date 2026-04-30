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
    private CommentId rootCommentId;
    private CommentId parentId;
    private String content;
    private CommentStatus status;
    private Integer likeCount;
    private LocalDateTime editedAt;
    private Integer editCount;
    private Boolean pinned;
    private LocalDateTime pinnedAt;
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
     * @param rootCommentId 根评论 ID
     * @param parentId 父评论 ID
     * @param content 评论内容
     * @return 评论聚合根
     */
    public static Comment create(Long id, ArticleId articleId, UserId userId, Long rootCommentId,
                                 Long parentId, String content) {
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
        comment.rootCommentId = rootCommentId != null && rootCommentId > 0 ? new CommentId(rootCommentId) : comment.id;
        comment.parentId = parentId != null && parentId > 0 ? new CommentId(parentId) : null;
        comment.content = content.trim();
        comment.status = CommentStatus.PUBLISHED;
        comment.likeCount = 0;
        comment.editedAt = null;
        comment.editCount = 0;
        comment.pinned = Boolean.FALSE;
        comment.pinnedAt = null;
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
     * @param rootCommentId 根评论 ID
     * @param parentId 父评论 ID
     * @param content 评论内容
     * @param status 评论状态
     * @param likeCount 点赞数
     * @param pinned 是否置顶
     * @param pinnedAt 置顶时间
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @return 评论聚合根
     */
    public static Comment restore(Long id, ArticleId articleId, UserId userId, Long rootCommentId,
                                 Long parentId, String content, CommentStatus status,
                                 Integer likeCount, LocalDateTime editedAt, Integer editCount,
                                 Boolean pinned, LocalDateTime pinnedAt,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        Comment comment = new Comment();
        comment.id = new CommentId(id);
        comment.articleId = articleId;
        comment.userId = userId;
        comment.rootCommentId = rootCommentId != null && rootCommentId > 0 ? new CommentId(rootCommentId) : comment.id;
        comment.parentId = parentId != null && parentId > 0 ? new CommentId(parentId) : null;
        comment.content = content;
        comment.status = status;
        comment.likeCount = likeCount == null ? 0 : likeCount;
        comment.editedAt = editedAt;
        comment.editCount = editCount == null ? 0 : editCount;
        comment.pinned = pinned != null && pinned;
        comment.pinnedAt = pinnedAt;
        comment.createdAt = createdAt;
        comment.updatedAt = updatedAt;
        return comment;
    }

    /**
     * 编辑评论内容（仅允许 10 分钟内编辑）。
     *
     * @param newContent 新内容
     */
    public void edit(String newContent) {
        if (CommentStatus.DELETED.equals(this.status)) {
            throw new DomainException(ErrorCode.CONFLICT, "评论已删除，无法编辑");
        }
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "评论内容不能为空");
        }
        if (newContent.length() > 1000) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "评论内容不能超过 1000 字符");
        }
        this.content = newContent.trim();
        this.editedAt = LocalDateTime.now();
        this.editCount = this.editCount == null ? 1 : this.editCount + 1;
        this.updatedAt = this.editedAt;
    }

    /**
     * 删除评论。
     */
    public void delete() {
        if (CommentStatus.DELETED.equals(this.status)) {
            throw new DomainException(ErrorCode.CONFLICT, "评论已删除");
        }
        this.status = CommentStatus.DELETED;
        this.pinned = Boolean.FALSE;
        this.pinnedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 点赞评论。
     */
    public void increaseLikeCount() {
        this.likeCount = this.likeCount == null ? 1 : this.likeCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消点赞。
     */
    public void decreaseLikeCount() {
        int currentLikeCount = this.likeCount == null ? 0 : this.likeCount;
        this.likeCount = currentLikeCount > 0 ? currentLikeCount - 1 : 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 置顶评论。
     */
    public void pin() {
        if (!isRootComment()) {
            throw new DomainException(ErrorCode.CONFLICT, "仅一级评论支持置顶");
        }
        this.pinned = Boolean.TRUE;
        this.pinnedAt = LocalDateTime.now();
        this.updatedAt = this.pinnedAt;
    }

    /**
     * 取消置顶评论。
     */
    public void unpin() {
        this.pinned = Boolean.FALSE;
        this.pinnedAt = null;
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
     * 获取根评论 ID。
     *
     * @return 根评论 ID
     */
    public Long getRootCommentId() {
        return rootCommentId != null ? rootCommentId.getValue() : null;
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
     * 获取点赞数。
     *
     * @return 点赞数
     */
    public Integer getLikeCount() {
        return likeCount == null ? 0 : likeCount;
    }

    /**
     * 获取最后编辑时间。
     *
     * @return 最后编辑时间
     */
    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    /**
     * 获取编辑次数。
     *
     * @return 编辑次数
     */
    public Integer getEditCount() {
        return editCount == null ? 0 : editCount;
    }

    /**
     * 是否置顶。
     *
     * @return 是否置顶
     */
    public boolean isPinned() {
        return Boolean.TRUE.equals(pinned);
    }

    /**
     * 获取置顶时间。
     *
     * @return 置顶时间
     */
    public LocalDateTime getPinnedAt() {
        return pinnedAt;
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

    /**
     * 是否一级评论。
     *
     * @return 是否一级评论
     */
    public boolean isRootComment() {
        return parentId == null;
    }

    public enum CommentStatus {
        PUBLISHED,
        DELETED
    }
}
