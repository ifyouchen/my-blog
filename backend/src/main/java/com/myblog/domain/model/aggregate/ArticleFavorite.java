package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ArticleFavoriteId;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 文章收藏聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleFavorite {

    /**
     * 收藏记录 ID
     */
    private ArticleFavoriteId id;

    /**
     * 被收藏的文章 ID
     */
    private ArticleId articleId;

    /**
     * 收藏用户 ID
     */
    private UserId userId;

    /**
     * 收藏创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 收藏最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示收藏有效
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private ArticleFavorite() {
    }

    /**
     * 创建文章收藏聚合根。
     *
     * @param id        收藏记录 ID
     * @param articleId 被收藏的文章 ID
     * @param userId    收藏用户 ID
     * @return 文章收藏聚合根
     */
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

    /**
     * 从持久化数据还原文章收藏聚合根。
     *
     * @param id        收藏记录 ID
     * @param articleId 被收藏的文章 ID
     * @param userId    收藏用户 ID
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param deletedAt 删除时间
     * @param version   乐观锁版本号
     * @return 文章收藏聚合根
     */
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

    /**
     * 取消收藏（软删除）。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 重新激活已取消的收藏记录。
     */
    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断收藏是否已取消。
     *
     * @return 已取消返回 true，否则返回 false
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 获取收藏记录 ID。
     *
     * @return 收藏记录 ID
     */
    public ArticleFavoriteId getId() {
        return id;
    }

    /**
     * 获取被收藏的文章 ID。
     *
     * @return 文章 ID
     */
    public ArticleId getArticleId() {
        return articleId;
    }

    /**
     * 获取收藏用户 ID。
     *
     * @return 收藏用户 ID
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * 获取收藏创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取收藏最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未取消则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}