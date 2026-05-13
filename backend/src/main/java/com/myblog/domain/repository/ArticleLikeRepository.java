package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.ArticleLike;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 文章点赞仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface ArticleLikeRepository {

    /**
     * 查询当前有效的点赞记录。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞记录
     */
    Optional<ArticleLike> findByArticleAndUser(ArticleId articleId, UserId userId);

    /**
     * 查询点赞记录，包含已取消的逻辑删除记录。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞记录
     */
    Optional<ArticleLike> findAnyByArticleAndUser(ArticleId articleId, UserId userId);

    /**
     * 判断用户是否已点赞文章。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    boolean exists(ArticleId articleId, UserId userId);

    /**
     * 保存点赞记录。
     *
     * @param articleLike 点赞聚合
     * @return 保存后的点赞聚合
     */
    ArticleLike save(ArticleLike articleLike);

    /**
     * 生成下一条点赞记录ID。
     *
     * @return 点赞记录ID
     */
    Long nextId();

    /**
     * 批量查询当前用户对多篇文章的点赞状态。
     *
     * @param articleIds 文章ID列表
     * @param userId 用户ID
     * @return 已点赞的文章ID集合
     */
    Set<Long> findLikedArticleIdsByUser(List<Long> articleIds, UserId userId);
}
