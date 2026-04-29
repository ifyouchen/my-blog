package com.myblog.application.dto;

/**
 * 创作者工作台概览。
 *
 * @author Codex
 * @since 1.0.0
 */
public class DashboardOverviewDTO {

    private int totalCount;
    private int draftCount;
    private int publishedCount;
    private int offlineCount;
    private int deletedCount;
    private long totalViewCount;
    private long totalLikeCount;
    private long totalFavoriteCount;
    private long totalCommentCount;
    private int followerCount;
    private Long latestArticleId;
    private String latestArticleTitle;
    private String latestArticleStatus;
    private String latestUpdatedAt;
    private String recommendedActionType;
    private String recommendedActionText;
    private String recommendedActionHint;
    private String recommendedActionRoute;

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

    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public Long getLatestArticleId() {
        return latestArticleId;
    }

    public void setLatestArticleId(Long latestArticleId) {
        this.latestArticleId = latestArticleId;
    }

    public String getLatestArticleTitle() {
        return latestArticleTitle;
    }

    public void setLatestArticleTitle(String latestArticleTitle) {
        this.latestArticleTitle = latestArticleTitle;
    }

    public String getLatestArticleStatus() {
        return latestArticleStatus;
    }

    public void setLatestArticleStatus(String latestArticleStatus) {
        this.latestArticleStatus = latestArticleStatus;
    }

    public String getLatestUpdatedAt() {
        return latestUpdatedAt;
    }

    public void setLatestUpdatedAt(String latestUpdatedAt) {
        this.latestUpdatedAt = latestUpdatedAt;
    }

    public String getRecommendedActionType() {
        return recommendedActionType;
    }

    public void setRecommendedActionType(String recommendedActionType) {
        this.recommendedActionType = recommendedActionType;
    }

    public String getRecommendedActionText() {
        return recommendedActionText;
    }

    public void setRecommendedActionText(String recommendedActionText) {
        this.recommendedActionText = recommendedActionText;
    }

    public String getRecommendedActionHint() {
        return recommendedActionHint;
    }

    public void setRecommendedActionHint(String recommendedActionHint) {
        this.recommendedActionHint = recommendedActionHint;
    }

    public String getRecommendedActionRoute() {
        return recommendedActionRoute;
    }

    public void setRecommendedActionRoute(String recommendedActionRoute) {
        this.recommendedActionRoute = recommendedActionRoute;
    }
}
