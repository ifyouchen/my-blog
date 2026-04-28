package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.valueobject.ArticleFavoriteId;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    List<ArticleFavorite> findPublishedByUserId(UserId userId, int page, int pageSize);

    /**
     * 统计用户收藏数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    int countByUserId(UserId userId);

    int countPublishedByUserId(UserId userId);

    /**
     * 批量查询当前用户对多篇文章的收藏状态。
     *
     * @param articleIds 文章ID列表
     * @param userId 用户ID
     * @return 已收藏的文章ID集合
     */
    Set<Long> findFavoritedArticleIdsByUser(List<Long> articleIds, UserId userId);
}
