package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ArticleLikeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章点赞 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ArticleLikeMapper {

    /**
     * 根据文章ID和用户ID查询有效点赞记录。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞数据对象
     */
    ArticleLikeDO selectByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 根据文章ID和用户ID查询点赞记录，包含已逻辑删除记录。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞数据对象
     */
    ArticleLikeDO selectAnyByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 统计用户对文章的有效点赞数。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 有效点赞数
     */
    int countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    List<ArticleLikeDO> selectByArticleId(@Param("articleId") Long articleId);

    int countByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据主键统计点赞记录。
     *
     * @param id 点赞记录ID
     * @return 记录数
     */
    int countById(@Param("id") Long id);

    Long selectNextId();

    int insert(ArticleLikeDO articleLikeDO);

    int update(ArticleLikeDO articleLikeDO);

    int deleteByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);
}
