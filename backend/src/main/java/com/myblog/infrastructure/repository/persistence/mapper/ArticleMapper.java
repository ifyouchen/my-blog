package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ArticleDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ArticleMapper {

    /**
     * 根据 ID 查询文章。
     *
     * @param id 文章 ID
     * @return 文章数据对象
     */
    ArticleDO selectById(@Param("id") Long id);

    /**
     * 查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectPublished(@Param("keyword") String keyword,
                                    @Param("category") String category,
                                    @Param("tag") String tag);

    /**
     * 根据 ID 统计文章数量。
     *
     * @param id 文章 ID
     * @return 文章数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个文章 ID。
     *
     * @return 下一个文章 ID
     */
    Long selectNextId();

    /**
     * 插入文章。
     *
     * @param article 文章数据对象
     * @return 影响行数
     */
    int insert(ArticleDO article);

    /**
     * 更新文章。
     *
     * @param article 文章数据对象
     * @return 影响行数
     */
    int update(ArticleDO article);

    /**
     * 查询文章标签名称列表。
     *
     * @param articleId 文章 ID
     * @return 标签名称列表
     */
    List<String> selectTagNamesByArticleId(@Param("articleId") Long articleId);

    /**
     * 逻辑删除文章标签。
     *
     * @param articleId 文章 ID
     * @return 影响行数
     */
    int logicDeleteTagsByArticleId(@Param("articleId") Long articleId);

    /**
     * 统计文章标签数量。
     *
     * @param articleId 文章 ID
     * @param tagName 标签名称
     * @return 标签数量
     */
    int countTag(@Param("articleId") Long articleId, @Param("tagName") String tagName);

    /**
     * 恢复文章标签。
     *
     * @param articleId 文章 ID
     * @param tagName 标签名称
     * @return 影响行数
     */
    int restoreTag(@Param("articleId") Long articleId, @Param("tagName") String tagName);

    /**
     * 插入文章标签。
     *
     * @param articleId 文章 ID
     * @param tagName 标签名称
     * @return 影响行数
     */
    int insertTag(@Param("articleId") Long articleId, @Param("tagName") String tagName);
}
