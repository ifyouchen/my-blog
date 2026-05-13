package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ArticleFavoriteDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章收藏 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ArticleFavoriteMapper {

    /**
     * 查询有效的文章收藏记录（未取消）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 文章收藏数据对象
     */
    ArticleFavoriteDO selectByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 查询文章收藏记录（包含已逻辑删除的记录）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 文章收藏数据对象
     */
    ArticleFavoriteDO selectByArticleAndUserIncludingDeleted(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 统计有效的文章收藏数量。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 收藏数量
     */
    int countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 分页查询用户的收藏列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 收藏列表
     */
    List<ArticleFavoriteDO> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 分页查询用户收藏的已发布文章列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 收藏列表
     */
    List<ArticleFavoriteDO> selectPublishedByUserId(@Param("userId") Long userId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    /**
     * 分页查询用户收藏的已发布文章列表（含关键字过滤）。
     *
     * @param userId   用户 ID
     * @param keyword  关键字
     * @param offset   偏移量
     * @param limit    限制数量
     * @return 收藏列表
     */
    List<ArticleFavoriteDO> selectPublishedByUserIdAndKeyword(@Param("userId") Long userId,
                                                               @Param("keyword") String keyword,
                                                               @Param("offset") int offset,
                                                               @Param("limit") int limit);

    /**
     * 统计用户的收藏总数。
     *
     * @param userId 用户 ID
     * @return 收藏总数
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计用户收藏的已发布文章数量。
     *
     * @param userId 用户 ID
     * @return 收藏数量
     */
    int countPublishedByUserId(@Param("userId") Long userId);

    /**
     * 统计用户收藏的已发布文章数量（含关键字过滤）。
     *
     * @param userId  用户 ID
     * @param keyword 关键字
     * @return 收藏数量
     */
    int countPublishedByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 根据主键统计收藏记录数量。
     *
     * @param id 收藏记录 ID
     * @return 记录数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个收藏记录 ID。
     *
     * @return 下一个收藏记录 ID
     */
    Long selectNextId();

    /**
     * 插入收藏记录。
     *
     * @param favoriteDO 收藏数据对象
     * @return 影响行数
     */
    int insert(ArticleFavoriteDO favoriteDO);

    /**
     * 插入或更新收藏记录（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param articleFavoriteDO 收藏数据对象
     * @return 影响行数
     */
    int insertOrUpdate(ArticleFavoriteDO articleFavoriteDO);

    /**
     * 更新收藏记录。
     *
     * @param favoriteDO 收藏数据对象
     * @return 影响行数
     */
    int update(ArticleFavoriteDO favoriteDO);

    /**
     * 删除指定用户对指定文章的收藏记录。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 影响行数
     */
    int deleteByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 批量查询用户对多篇文章的收藏状态，返回已收藏的文章 ID 列表。
     *
     * @param articleIds 文章 ID 列表
     * @param userId     用户 ID
     * @return 已收藏的文章 ID 列表
     */
    List<Long> selectFavoritedArticleIdsByUser(@Param("articleIds") List<Long> articleIds, @Param("userId") Long userId);
}
