package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 作者文章聚合数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthorArticleMetricsDO {

    /**
     * 文章总数量
     */
    private Integer articleCount;

    /**
     * 累计阅读量
     */
    private Long totalViews;

    /**
     * 累计点赞量
     */
    private Long totalLikes;

    /**
     * 累计收藏量
     */
    private Long totalFavorites;

    /**
     * 累计评论量
     */
    private Long totalComments;

    /**
     * 草稿状态文章数量
     */
    private Integer draftCount;

    /**
     * 已发布状态文章数量
     */
    private Integer publishedCount;

    /**
     * 已下线状态文章数量
     */
    private Integer offlineCount;

    /**
     * 已删除状态文章数量
     */
    private Integer deletedCount;

    /**
     * 获取文章总数量。
     *
     * @return 文章总数量
     */
    public Integer getArticleCount() {
        return articleCount;
    }

    /**
     * 设置文章总数量。
     *
     * @param articleCount 文章总数量
     */
    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    /**
     * 获取累计阅读量。
     *
     * @return 累计阅读量
     */
    public Long getTotalViews() {
        return totalViews;
    }

    /**
     * 设置累计阅读量。
     *
     * @param totalViews 累计阅读量
     */
    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    /**
     * 获取累计点赞量。
     *
     * @return 累计点赞量
     */
    public Long getTotalLikes() {
        return totalLikes;
    }

    /**
     * 设置累计点赞量。
     *
     * @param totalLikes 累计点赞量
     */
    public void setTotalLikes(Long totalLikes) {
        this.totalLikes = totalLikes;
    }

    /**
     * 获取累计收藏量。
     *
     * @return 累计收藏量
     */
    public Long getTotalFavorites() {
        return totalFavorites;
    }

    /**
     * 设置累计收藏量。
     *
     * @param totalFavorites 累计收藏量
     */
    public void setTotalFavorites(Long totalFavorites) {
        this.totalFavorites = totalFavorites;
    }

    /**
     * 获取累计评论量。
     *
     * @return 累计评论量
     */
    public Long getTotalComments() {
        return totalComments;
    }

    /**
     * 设置累计评论量。
     *
     * @param totalComments 累计评论量
     */
    public void setTotalComments(Long totalComments) {
        this.totalComments = totalComments;
    }

    /**
     * 获取草稿状态文章数量。
     *
     * @return 草稿数量
     */
    public Integer getDraftCount() {
        return draftCount;
    }

    /**
     * 设置草稿状态文章数量。
     *
     * @param draftCount 草稿数量
     */
    public void setDraftCount(Integer draftCount) {
        this.draftCount = draftCount;
    }

    /**
     * 获取已发布状态文章数量。
     *
     * @return 已发布数量
     */
    public Integer getPublishedCount() {
        return publishedCount;
    }

    /**
     * 设置已发布状态文章数量。
     *
     * @param publishedCount 已发布数量
     */
    public void setPublishedCount(Integer publishedCount) {
        this.publishedCount = publishedCount;
    }

    /**
     * 获取已下线状态文章数量。
     *
     * @return 已下线数量
     */
    public Integer getOfflineCount() {
        return offlineCount;
    }

    /**
     * 设置已下线状态文章数量。
     *
     * @param offlineCount 已下线数量
     */
    public void setOfflineCount(Integer offlineCount) {
        this.offlineCount = offlineCount;
    }

    /**
     * 获取已删除状态文章数量。
     *
     * @return 已删除数量
     */
    public Integer getDeletedCount() {
        return deletedCount;
    }

    /**
     * 设置已删除状态文章数量。
     *
     * @param deletedCount 已删除数量
     */
    public void setDeletedCount(Integer deletedCount) {
        this.deletedCount = deletedCount;
    }
}
