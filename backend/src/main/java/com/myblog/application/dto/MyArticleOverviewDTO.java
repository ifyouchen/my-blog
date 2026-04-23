package com.myblog.application.dto;

/**
 * 我的创作概览数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class MyArticleOverviewDTO {

    private int totalCount;
    private int draftCount;
    private int publishedCount;
    private int offlineCount;
    private int deletedCount;
    private long totalViewCount;
    private long totalLikeCount;
    private long totalFavoriteCount;
    private long totalCommentCount;
    private String latestArticleTitle;
    private String latestUpdatedAt;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getDraftCount() {
        return draftCount;
    }

    public void setDraftCount(int draftCount) {
        this.draftCount = draftCount;
    }

    public int getPublishedCount() {
        return publishedCount;
    }

    public void setPublishedCount(int publishedCount) {
        this.publishedCount = publishedCount;
    }

    public int getOfflineCount() {
        return offlineCount;
    }

    public void setOfflineCount(int offlineCount) {
        this.offlineCount = offlineCount;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public long getTotalViewCount() {
        return totalViewCount;
    }

    public void setTotalViewCount(long totalViewCount) {
        this.totalViewCount = totalViewCount;
    }

    public long getTotalLikeCount() {
        return totalLikeCount;
    }

    public void setTotalLikeCount(long totalLikeCount) {
        this.totalLikeCount = totalLikeCount;
    }

    public long getTotalFavoriteCount() {
        return totalFavoriteCount;
    }

    public void setTotalFavoriteCount(long totalFavoriteCount) {
        this.totalFavoriteCount = totalFavoriteCount;
    }

    public long getTotalCommentCount() {
        return totalCommentCount;
    }

    public void setTotalCommentCount(long totalCommentCount) {
        this.totalCommentCount = totalCommentCount;
    }

    public String getLatestArticleTitle() {
        return latestArticleTitle;
    }

    public void setLatestArticleTitle(String latestArticleTitle) {
        this.latestArticleTitle = latestArticleTitle;
    }

    public String getLatestUpdatedAt() {
        return latestUpdatedAt;
    }

    public void setLatestUpdatedAt(String latestUpdatedAt) {
        this.latestUpdatedAt = latestUpdatedAt;
    }
}
