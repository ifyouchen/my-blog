package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ArticleFavoriteId;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 文章收藏聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleFavorite {

    private ArticleFavoriteId id;
    private ArticleId articleId;
    private UserId userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private ArticleFavorite() {
    }

    public static ArticleFavorite create(Long id, ArticleId articleId, UserId userId) {
        ArticleFavorite favorite = new ArticleFavorite();
        favorite.id = new ArticleFavoriteId(id);
        favorite.articleId = articleId;
        favorite.userId = userId;
        favorite.createdAt = LocalDateTime.now();
        favorite.updatedAt = favorite.createdAt;
        favorite.deletedAt = null;
        favorite.version = 0;
        return favorite;
    }

    public static ArticleFavorite restore(Long id, ArticleId articleId, UserId userId,
                                           LocalDateTime createdAt, LocalDateTime updatedAt,
                                           LocalDateTime deletedAt, Integer version) {
        ArticleFavorite favorite = new ArticleFavorite();
        favorite.id = new ArticleFavoriteId(id);
        favorite.articleId = articleId;
        favorite.userId = userId;
        favorite.createdAt = createdAt;
        favorite.updatedAt = updatedAt;
        favorite.deletedAt = deletedAt;
        favorite.version = version;
        return favorite;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public ArticleFavoriteId getId() {
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