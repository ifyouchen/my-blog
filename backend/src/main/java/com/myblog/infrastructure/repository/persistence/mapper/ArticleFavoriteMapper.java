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

    int countByUserId(@Param("userId") Long userId);

    int countById(@Param("id") Long id);

    Long selectNextId();

    int insert(ArticleFavoriteDO favoriteDO);

    int update(ArticleFavoriteDO favoriteDO);

    int deleteByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);
}
