package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.ArticleLike;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.infrastructure.repository.persistence.entity.ArticleLikeDO;

/**
 * 文章点赞持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ArticleLikePersistenceConverter {

    private ArticleLikePersistenceConverter() {
    }

    /**
     * 将文章点赞数据对象转换为领域对象。
     *
     * @param articleLikeDO 文章点赞数据对象
     * @return 文章点赞领域对象，若 articleLikeDO 为 null 则返回 null
     */
    public static ArticleLike toDomain(ArticleLikeDO articleLikeDO) {
        if (articleLikeDO == null) {
            return null;
        }
        return ArticleLike.restore(
            articleLikeDO.getId(),
            new ArticleId(articleLikeDO.getArticleId()),
            new UserId(articleLikeDO.getUserId()),
            articleLikeDO.getCreatedAt(),
            articleLikeDO.getUpdatedAt(),
            articleLikeDO.getDeletedAt(),
            articleLikeDO.getVersion()
        );
    }

    /**
     * 将文章点赞领域对象转换为数据对象。
     *
     * @param articleLike 文章点赞领域对象
     * @return 文章点赞数据对象
     */
    public static ArticleLikeDO toData(ArticleLike articleLike) {
        ArticleLikeDO articleLikeDO = new ArticleLikeDO();
        articleLikeDO.setId(articleLike.getId().getValue());
        articleLikeDO.setArticleId(articleLike.getArticleId().getValue());
        articleLikeDO.setUserId(articleLike.getUserId().getValue());
        articleLikeDO.setCreatedAt(articleLike.getCreatedAt());
        articleLikeDO.setUpdatedAt(articleLike.getUpdatedAt());
        articleLikeDO.setDeletedAt(articleLike.getDeletedAt());
        articleLikeDO.setVersion(articleLike.getVersion());
        return articleLikeDO;
    }
}