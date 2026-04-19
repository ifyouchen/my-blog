package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.infrastructure.repository.persistence.entity.ArticleDO;
import com.myblog.shared.enums.ArticleStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ArticlePersistenceConverter {

    private ArticlePersistenceConverter() {
    }

    /**
     * 将文章数据对象转换为领域对象。
     *
     * @param articleDO 文章数据对象
     * @param tags 标签列表
     * @return 文章领域对象
     */
    public static Article toDomain(ArticleDO articleDO, List<String> tags) {
        if (articleDO == null) {
            return null;
        }
        List<String> safeTags = tags == null ? new ArrayList<String>() : tags;
        return Article.restore(
            articleDO.getId(),
            new UserId(articleDO.getAuthorId()),
            articleDO.getTitle(),
            articleDO.getSummary(),
            articleDO.getContent(),
            articleDO.getCoverUrl(),
            articleDO.getCategory(),
            safeTags,
            ArticleStatus.valueOf(articleDO.getStatus()),
            intValue(articleDO.getViewCount()),
            intValue(articleDO.getLikeCount()),
            intValue(articleDO.getFavoriteCount()),
            intValue(articleDO.getCommentCount()),
            articleDO.getPublishedAt(),
            articleDO.getCreatedAt(),
            articleDO.getUpdatedAt()
        );
    }

    /**
     * 将文章领域对象转换为数据对象。
     *
     * @param article 文章领域对象
     * @return 文章数据对象
     */
    public static ArticleDO toData(Article article) {
        ArticleDO articleDO = new ArticleDO();
        articleDO.setId(article.getId().getValue());
        articleDO.setAuthorId(article.getAuthorId().getValue());
        articleDO.setTitle(article.getTitle());
        articleDO.setSummary(article.getSummary());
        articleDO.setContent(article.getContent());
        articleDO.setCoverUrl(article.getCoverUrl());
        articleDO.setCategory(article.getCategory());
        articleDO.setStatus(article.getStatus().name());
        articleDO.setViewCount(article.getViewCount());
        articleDO.setLikeCount(article.getLikeCount());
        articleDO.setFavoriteCount(article.getFavoriteCount());
        articleDO.setCommentCount(article.getCommentCount());
        articleDO.setPublishedAt(article.getPublishedAt());
        articleDO.setCreatedAt(article.getCreatedAt());
        articleDO.setUpdatedAt(article.getUpdatedAt());
        return articleDO;
    }

    private static int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }
}
