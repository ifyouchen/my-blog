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

    ArticleFavoriteDO selectByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    ArticleFavoriteDO selectByArticleAndUserIncludingDeleted(@Param("articleId") Long articleId, @Param("userId") Long userId);

    int countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    List<ArticleFavoriteDO> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    List<ArticleFavoriteDO> selectPublishedByUserId(@Param("userId") Long userId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    List<ArticleFavoriteDO> selectPublishedByUserIdAndKeyword(@Param("userId") Long userId,
                                                               @Param("keyword") String keyword,
                                                               @Param("offset") int offset,
                                                               @Param("limit") int limit);

    int countByUserId(@Param("userId") Long userId);

    int countPublishedByUserId(@Param("userId") Long userId);

    int countPublishedByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    int countById(@Param("id") Long id);

    Long selectNextId();

    int insert(ArticleFavoriteDO favoriteDO);
    int insertOrUpdate(ArticleFavoriteDO articleFavoriteDO);


    int update(ArticleFavoriteDO favoriteDO);

    int deleteByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 批量查询用户对多篇文章的收藏状态，返回已收藏的文章ID。
     */
    List<Long> selectFavoritedArticleIdsByUser(@Param("articleIds") List<Long> articleIds, @Param("userId") Long userId);
}
