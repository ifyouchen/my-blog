package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.infrastructure.repository.persistence.entity.ArticleFavoriteDO;

/**
 * 文章收藏持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ArticleFavoritePersistenceConverter {

    /**
     * 私有构造函数，防止实例化。
     */
    private ArticleFavoritePersistenceConverter() {
    }

    /**
     * 将文章收藏数据对象转换为领域对象。
     *
     * @param favoriteDO 文章收藏数据对象
     * @return 文章收藏领域对象，若 favoriteDO 为 null 则返回 null
     */
    public static ArticleFavorite toDomain(ArticleFavoriteDO favoriteDO) {
        if (favoriteDO == null) {
            return null;
        }
        return ArticleFavorite.restore(
            favoriteDO.getId(),
            new ArticleId(favoriteDO.getArticleId()),
            new UserId(favoriteDO.getUserId()),
            favoriteDO.getCreatedAt(),
            favoriteDO.getUpdatedAt(),
            favoriteDO.getDeletedAt(),
            favoriteDO.getVersion()
        );
    }

    /**
     * 将文章收藏领域对象转换为数据对象。
     *
     * @param favorite 文章收藏领域对象
     * @return 文章收藏数据对象
     */
    public static ArticleFavoriteDO toData(ArticleFavorite favorite) {
        ArticleFavoriteDO favoriteDO = new ArticleFavoriteDO();
        favoriteDO.setId(favorite.getId().getValue());
        favoriteDO.setArticleId(favorite.getArticleId().getValue());
        favoriteDO.setUserId(favorite.getUserId().getValue());
        favoriteDO.setCreatedAt(favorite.getCreatedAt());
        favoriteDO.setUpdatedAt(favorite.getUpdatedAt());
        favoriteDO.setDeletedAt(favorite.getDeletedAt());
        favoriteDO.setVersion(favorite.getVersion());
        return favoriteDO;
    }
}