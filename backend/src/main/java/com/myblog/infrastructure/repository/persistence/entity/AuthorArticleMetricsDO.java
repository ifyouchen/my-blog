package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 作者文章聚合数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthorArticleMetricsDO {

    private Integer articleCount;
    private Long totalViews;
    private Long totalLikes;
    private Long totalFavorites;
    private Long totalComments;
    private Integer draftCount;
    private Integer publishedCount;
    private Integer offlineCount;
    private Integer deletedCount;

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    public Long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    public Long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(Long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public Long getTotalFavorites() {
        return totalFavorites;
    }

    public void setTotalFavorites(Long totalFavorites) {
        this.totalFavorites = totalFavorites;
    }

    public Long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(Long totalComments) {
        this.totalComments = totalComments;
    }

    public Integer getDraftCount() {
        return draftCount;
    }

    public void setDraftCount(Integer draftCount) {
        this.draftCount = draftCount;
    }

    public Integer getPublishedCount() {
        return publishedCount;
    }

    public void setPublishedCount(Integer publishedCount) {
        this.publishedCount = publishedCount;
    }

    public Integer getOfflineCount() {
        return offlineCount;
    }

    public void setOfflineCount(Integer offlineCount) {
        this.offlineCount = offlineCount;
    }

    public Integer getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(Integer deletedCount) {
        this.deletedCount = deletedCount;
    }
}
