package com.myblog.interfaces.rest.dto.request;

/**
 * 更新学习进度请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UpdateLearningProgressRequest {

    private String assetType;
    private Long assetId;
    private Long articleId;
    private Boolean completed;

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

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
