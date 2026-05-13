package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户在专题/专栏中的学习进度。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class LearningProgressDTO {

    /** 资产类型（TOPIC / COLUMN） */
    private String assetType;
    /** 资产 ID */
    private Long assetId;
    /** 已完成文章数 */
    private int completedCount;
    /** 总文章数 */
    private int totalCount;
    /** 进度百分比 */
    private int progressPercent;
    /** 已完成文章 ID 列表 */
    private List<Long> completedArticleIds = new ArrayList<Long>();
    /** 下一篇待学习文章 ID */
    private Long nextArticleId;
    /** 下一篇待学习文章 */
    private ArticleDTO nextArticle;

    /**
     * 获取资产类型。
     *
     * @return 资产类型
     */
    public String getAssetType() {
        return assetType;
    }

    /**
     * 设置资产类型。
     *
     * @param assetType 资产类型
     */
    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    /**
     * 获取资产 ID。
     *
     * @return 资产 ID
     */
    public Long getAssetId() {
        return assetId;
    }

    /**
     * 设置资产 ID。
     *
     * @param assetId 资产 ID
     */
    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    /**
     * 获取已完成文章数。
     *
     * @return 已完成文章数
     */
    public int getCompletedCount() {
        return completedCount;
    }

    /**
     * 设置已完成文章数。
     *
     * @param completedCount 已完成文章数
     */
    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    /**
     * 获取总文章数。
     *
     * @return 总文章数
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 设置总文章数。
     *
     * @param totalCount 总文章数
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * 获取进度百分比。
     *
     * @return 进度百分比
     */
    public int getProgressPercent() {
        return progressPercent;
    }

    /**
     * 设置进度百分比。
     *
     * @param progressPercent 进度百分比
     */
    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * 获取已完成文章 ID 列表。
     *
     * @return 已完成文章 ID 列表
     */
    public List<Long> getCompletedArticleIds() {
        return completedArticleIds;
    }

    /**
     * 设置已完成文章 ID 列表。
     *
     * @param completedArticleIds 已完成文章 ID 列表
     */
    public void setCompletedArticleIds(List<Long> completedArticleIds) {
        this.completedArticleIds = completedArticleIds;
    }

    /**
     * 获取下一篇待学习文章 ID。
     *
     * @return 下一篇待学习文章 ID
     */
    public Long getNextArticleId() {
        return nextArticleId;
    }

    /**
     * 设置下一篇待学习文章 ID。
     *
     * @param nextArticleId 下一篇待学习文章 ID
     */
    public void setNextArticleId(Long nextArticleId) {
        this.nextArticleId = nextArticleId;
    }

    /**
     * 获取下一篇待学习文章。
     *
     * @return 下一篇待学习文章
     */
    public ArticleDTO getNextArticle() {
        return nextArticle;
    }

    /**
     * 设置下一篇待学习文章。
     *
     * @param nextArticle 下一篇待学习文章
     */
    public void setNextArticle(ArticleDTO nextArticle) {
        this.nextArticle = nextArticle;
    }
}
