package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户在专题/专栏中的学习进度。
 *
 * @author Codex
 * @since 1.0.0
 */
public class LearningProgressDTO {

    private String assetType;
    private Long assetId;
    private int completedCount;
    private int totalCount;
    private int progressPercent;
    private List<Long> completedArticleIds = new ArrayList<Long>();
    private Long nextArticleId;
    private ArticleDTO nextArticle;

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public List<Long> getCompletedArticleIds() {
        return completedArticleIds;
    }

    public void setCompletedArticleIds(List<Long> completedArticleIds) {
        this.completedArticleIds = completedArticleIds;
    }

    public Long getNextArticleId() {
        return nextArticleId;
    }

    public void setNextArticleId(Long nextArticleId) {
        this.nextArticleId = nextArticleId;
    }

    public ArticleDTO getNextArticle() {
        return nextArticle;
    }

    public void setNextArticle(ArticleDTO nextArticle) {
        this.nextArticle = nextArticle;
    }
}
