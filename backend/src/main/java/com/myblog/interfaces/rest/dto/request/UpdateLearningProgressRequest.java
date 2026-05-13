package com.myblog.interfaces.rest.dto.request;

/**
 * 更新学习进度请求.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UpdateLearningProgressRequest {

    /** 资产类型（如 article、column、topic）. */
    private String assetType;

    /** 资产ID. */
    private Long assetId;

    /** 关联文章ID. */
    private Long articleId;

    /** 是否已完成. */
    private Boolean completed;

    /**
     * 获取资产类型.
     *
     * @return 资产类型
     */
    public String getAssetType() {
        return assetType;
    }

    /**
     * 设置资产类型.
     *
     * @param assetType 资产类型
     */
    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    /**
     * 获取资产ID.
     *
     * @return 资产ID
     */
    public Long getAssetId() {
        return assetId;
    }

    /**
     * 设置资产ID.
     *
     * @param assetId 资产ID
     */
    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    /**
     * 获取关联文章ID.
     *
     * @return 文章ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置关联文章ID.
     *
     * @param articleId 文章ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取是否已完成.
     *
     * @return 是否已完成
     */
    public Boolean getCompleted() {
        return completed;
    }

    /**
     * 设置是否已完成.
     *
     * @param completed 是否已完成
     */
    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
