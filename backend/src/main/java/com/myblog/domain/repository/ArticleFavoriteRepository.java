package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.valueobject.ArticleFavoriteId;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 文章收藏仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ArticleFavoriteRepository {

    Optional<ArticleFavorite> findByArticleAndUser(ArticleId articleId, UserId userId);

    Optional<ArticleFavorite> findByArticleAndUserIncludingDeleted(ArticleId articleId, UserId userId);

    boolean exists(ArticleId articleId, UserId userId);

    ArticleFavorite save(ArticleFavorite favorite);

    Long nextId();

    List<ArticleFavorite> findByUserId(UserId userId, int page, int pageSize);

    /**
     * 统计用户收藏数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    int countByUserId(UserId userId);
}
