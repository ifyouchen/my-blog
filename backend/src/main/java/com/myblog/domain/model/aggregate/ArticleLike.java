package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.ArticleLikeId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 文章点赞聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleLike {

    /**
     * 点赞记录 ID
     */
    private ArticleLikeId id;

    /**
     * 被点赞的文章 ID
     */
    private ArticleId articleId;

    /**
     * 点赞用户 ID
     */
    private UserId userId;

    /**
     * 点赞创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 点赞最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示点赞有效
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private ArticleLike() {
    }

    /**
     * 创建文章点赞聚合根。
     *
     * @param id        点赞记录 ID
     * @param articleId 被点赞的文章 ID
     * @param userId    点赞用户 ID
     * @return 文章点赞聚合根
     */
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

    /**
     * 从持久化数据还原文章点赞聚合根。
     *
     * @param id        点赞记录 ID
     * @param articleId 被点赞的文章 ID
     * @param userId    点赞用户 ID
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param deletedAt 删除时间
     * @param version   乐观锁版本号
     * @return 文章点赞聚合根
     */
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
    }

    /**
     * 取消点赞（软删除）。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断点赞记录是否已逻辑删除。
     *
     * @return 已删除返回 true，否则返回 false
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 获取点赞记录 ID。
     *
     * @return 点赞记录 ID
     */
    public ArticleLikeId getId() {
        return id;
    }

    /**
     * 获取被点赞的文章 ID。
     *
     * @return 文章 ID
     */
    public ArticleId getArticleId() {
        return articleId;
    }

    /**
     * 获取点赞用户 ID。
     *
     * @return 点赞用户 ID
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * 获取点赞创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取点赞最后更新时间。
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
