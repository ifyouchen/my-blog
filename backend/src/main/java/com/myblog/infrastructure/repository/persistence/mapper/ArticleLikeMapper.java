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

    /**
     * 根据文章 ID 查询点赞列表。
     *
     * @param articleId 文章 ID
     * @return 点赞列表
     */
    List<ArticleLikeDO> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据文章 ID 统计点赞数量。
     *
     * @param articleId 文章 ID
     * @return 点赞数量
     */
    int countByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据主键统计点赞记录。
     *
     * @param id 点赞记录ID
     * @return 记录数
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个点赞记录 ID。
     *
     * @return 下一个点赞记录 ID
     */
    Long selectNextId();

    /**
     * 插入点赞记录。
     *
     * @param articleLikeDO 点赞数据对象
     * @return 影响行数
     */
    int insert(ArticleLikeDO articleLikeDO);
    /**
     * 插入或更新点赞记录（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param articleLikeDO 点赞数据对象
     * @return 影响行数
     */
    int insertOrUpdate(ArticleLikeDO articleLikeDO);


    /**
     * 更新点赞记录。
     *
     * @param articleLikeDO 点赞数据对象
     * @return 影响行数
     */
    int update(ArticleLikeDO articleLikeDO);

    /**
     * 根据文章 ID 和用户 ID 删除点赞记录。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 影响行数
     */
    int deleteByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /**
     * 批量查询用户对多篇文章的点赞状态，返回已点赞的文章ID。
     */
    List<Long> selectLikedArticleIdsByUser(@Param("articleIds") List<Long> articleIds, @Param("userId") Long userId);
}
