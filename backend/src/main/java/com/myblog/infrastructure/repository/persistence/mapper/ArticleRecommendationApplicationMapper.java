package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.domain.model.valueobject.ArticleRecommendationApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 首页推荐申请 MyBatis Mapper.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Mapper
public interface ArticleRecommendationApplicationMapper {

    /**
     * 插入申请.
     *
     * @param application 申请记录
     * @return 影响行数
     */
    int insert(ArticleRecommendationApplication application);

    /**
     * 更新申请状态.
     *
     * @param application 申请记录
     * @return 影响行数
     */
    int updateById(ArticleRecommendationApplication application);

    /**
     * 查询文章的待审核申请.
     *
     * @param articleId 文章ID
     * @return 申请记录
     */
    Optional<ArticleRecommendationApplication> selectPendingByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据主键查询.
     *
     * @param id 主键ID
     * @return 申请记录
     */
    Optional<ArticleRecommendationApplication> selectById(@Param("id") Long id);

    /**
     * 批量查询文章申请记录.
     *
     * @param articleIds 文章ID列表
     * @return 申请记录列表
     */
    List<ArticleRecommendationApplication> selectByArticleIds(@Param("articleIds") List<Long> articleIds);

    /**
     * 管理端总数查询.
     *
     * @param status 状态
     * @return 总数
     */
    long countForAdmin(@Param("status") String status);

    /**
     * 管理端分页查询.
     *
     * @param status 状态
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 申请记录列表
     */
    List<ArticleRecommendationApplication> selectPageForAdmin(@Param("status") String status,
                                                              @Param("offset") int offset,
                                                              @Param("limit") int limit);
}
