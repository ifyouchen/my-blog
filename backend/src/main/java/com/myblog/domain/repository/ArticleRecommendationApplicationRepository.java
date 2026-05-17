package com.myblog.domain.repository;

import com.myblog.domain.model.valueobject.ArticleRecommendationApplication;

import java.util.List;
import java.util.Optional;

/**
 * 首页推荐申请仓储接口.
 *
 * @author Codex
 * @since 2026-05-17
 */
public interface ArticleRecommendationApplicationRepository {

    /**
     * 保存新申请.
     *
     * @param application 申请记录
     */
    void save(ArticleRecommendationApplication application);

    /**
     * 更新申请状态.
     *
     * @param application 申请记录
     * @return true 表示更新成功
     */
    boolean update(ArticleRecommendationApplication application);

    /**
     * 查询指定文章的待审核申请.
     *
     * @param articleId 文章ID
     * @return 待审核申请
     */
    Optional<ArticleRecommendationApplication> findPendingByArticleId(Long articleId);

    /**
     * 根据主键查询.
     *
     * @param id 主键ID
     * @return 申请记录
     */
    Optional<ArticleRecommendationApplication> findById(Long id);

    /**
     * 批量查询文章申请记录.
     *
     * @param articleIds 文章ID列表
     * @return 申请记录列表
     */
    List<ArticleRecommendationApplication> findByArticleIds(List<Long> articleIds);

    /**
     * 管理端分页总数.
     *
     * @param status 状态筛选
     * @return 总数
     */
    long countForAdmin(String status);

    /**
     * 管理端分页查询.
     *
     * @param status 状态筛选
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 申请记录列表
     */
    List<ArticleRecommendationApplication> findPageForAdmin(String status, int offset, int limit);
}
