package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.ArticleFavorite;
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

    /**
     * 查询当前有效的文章收藏记录。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 收藏记录 Optional
     */
    Optional<ArticleFavorite> findByArticleAndUser(ArticleId articleId, UserId userId);

    /**
     * 查询文章收藏记录（包含已取消/逻辑删除的记录）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 收藏记录 Optional
     */
    Optional<ArticleFavorite> findByArticleAndUserIncludingDeleted(ArticleId articleId, UserId userId);

    /**
     * 判断用户是否已收藏文章（仅有效记录）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 是否已收藏
     */
    boolean exists(ArticleId articleId, UserId userId);

    /**
     * 保存文章收藏记录（新增或更新）。
     *
     * @param favorite 收藏聚合根
     * @return 保存后的收藏记录
     */
    ArticleFavorite save(ArticleFavorite favorite);

    /**
     * 生成下一个收藏记录 ID。
     *
     * @return 收藏记录 ID
     */
    Long nextId();

    /**
     * 分页查询用户的收藏列表。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 收藏记录列表
     */
    List<ArticleFavorite> findByUserId(UserId userId, int page, int pageSize);

    /**
     * 分页查询用户已发布文章的收藏列表。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 收藏记录列表
     */
    List<ArticleFavorite> findPublishedByUserId(UserId userId, int page, int pageSize);

    /**
     * 分页查询用户按关键字筛选的已发布文章收藏列表。
     *
     * @param userId   用户 ID
     * @param keyword  关键字
     * @param page     页码
     * @param pageSize 每页大小
     * @return 收藏记录列表
     */
    List<ArticleFavorite> findPublishedByUserIdAndKeyword(UserId userId, String keyword, int page, int pageSize);

    /**
     * 统计用户收藏数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    int countByUserId(UserId userId);

    /**
     * 统计用户已发布文章的收藏数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    int countPublishedByUserId(UserId userId);

    /**
     * 统计用户按关键字筛选的已发布文章收藏数量。
     *
     * @param userId  用户 ID
     * @param keyword 关键字
     * @return 收藏数量
     */
    int countPublishedByUserIdAndKeyword(UserId userId, String keyword);

    /**
     * 批量查询当前用户对多篇文章的收藏状态。
     *
     * @param articleIds 文章ID列表
     * @param userId 用户ID
     * @return 已收藏的文章ID集合
     */
    Set<Long> findFavoritedArticleIdsByUser(List<Long> articleIds, UserId userId);
}
