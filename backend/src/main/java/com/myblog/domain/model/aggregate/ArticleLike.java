package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.ArticleLikeId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 文章点赞聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleLike {

    private ArticleLikeId id;
    private ArticleId articleId;
    private UserId userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private ArticleLike() {
    }

    public static ArticleLike create(Long id, ArticleId articleId, UserId userId) {
        ArticleLike like = new ArticleLike();
        like.id = new ArticleLikeId(id);
        like.articleId = articleId;
        like.userId = userId;
        like.createdAt = LocalDateTime.now();
        like.updatedAt = like.createdAt;
        like.deletedAt = null;
        like.version = 0;
        return like;
    }

    public static ArticleLike restore(Long id, ArticleId articleId, UserId userId,
                                     LocalDateTime createdAt, LocalDateTime updatedAt,
                                     LocalDateTime deletedAt, Integer version) {
        ArticleLike like = new ArticleLike();
        like.id = new ArticleLikeId(id);
        like.articleId = articleId;
        like.userId = userId;
        like.createdAt = createdAt;
        like.updatedAt = updatedAt;
        like.deletedAt = deletedAt;
        like.version = version;
        return like;
    }

    /**
     * 重新激活已取消的点赞记录。
     */
    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    /**
     * 取消点赞。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    /**
     * 判断点赞记录是否已逻辑删除。
     *
     * @return 是否已删除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public ArticleLikeId getId() {
        return id;
    }

    public ArticleId getArticleId() {
        return articleId;
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
