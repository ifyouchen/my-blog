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

    private ArticleFavoritePersistenceConverter() {
    }

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