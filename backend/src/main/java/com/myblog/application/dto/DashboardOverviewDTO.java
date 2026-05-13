package com.myblog.application.dto;

/**
 * 创作者工作台概览。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class DashboardOverviewDTO {

    /** 文章总数 */
    private int totalCount;
    /** 草稿数 */
    private int draftCount;
    /** 已发布数 */
    private int publishedCount;
    /** 已下线数 */
    private int offlineCount;
    /** 已删除数 */
    private int deletedCount;
    /** 总阅读量 */
    private long totalViewCount;
    /** 总获赞数 */
    private long totalLikeCount;
    /** 总收藏数 */
    private long totalFavoriteCount;
    /** 总评论数 */
    private long totalCommentCount;
    /** 粉丝数 */
    private int followerCount;
    /** 最新文章 ID */
    private Long latestArticleId;
    /** 最新文章标题 */
    private String latestArticleTitle;
    /** 最新文章状态 */
    private String latestArticleStatus;
    /** 最新更新时间 */
    private String latestUpdatedAt;
    /** 推荐操作类型 */
    private String recommendedActionType;
    /** 推荐操作文案 */
    private String recommendedActionText;
    /** 推荐操作提示 */
    private String recommendedActionHint;
    /** 推荐操作路由 */
    private String recommendedActionRoute;

    /**
     * 获取文章总数。
     *
     * @return 文章总数
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 设置文章总数。
     *
     * @param totalCount 文章总数
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * 获取草稿数。
     *
     * @return 草稿数
     */
    public int getDraftCount() {
        return draftCount;
    }

    /**
     * 设置草稿数。
     *
     * @param draftCount 草稿数
     */
    public void setDraftCount(int draftCount) {
        this.draftCount = draftCount;
    }

    /**
     * 获取已发布数。
     *
     * @return 已发布数
     */
    public int getPublishedCount() {
        return publishedCount;
    }

    /**
     * 设置已发布数。
     *
     * @param publishedCount 已发布数
     */
    public void setPublishedCount(int publishedCount) {
        this.publishedCount = publishedCount;
    }

    /**
     * 获取已下线数。
     *
     * @return 已下线数
     */
    public int getOfflineCount() {
        return offlineCount;
    }

    /**
     * 设置已下线数。
     *
     * @param offlineCount 已下线数
     */
    public void setOfflineCount(int offlineCount) {
        this.offlineCount = offlineCount;
    }

    /**
     * 获取已删除数。
     *
     * @return 已删除数
     */
    public int getDeletedCount() {
        return deletedCount;
    }

    /**
     * 设置已删除数。
     *
     * @param deletedCount 已删除数
     */
    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    /**
     * 获取总阅读量。
     *
     * @return 总阅读量
     */
    public long getTotalViewCount() {
        return totalViewCount;
    }

    /**
     * 设置总阅读量。
     *
     * @param totalViewCount 总阅读量
     */
    public void setTotalViewCount(long totalViewCount) {
        this.totalViewCount = totalViewCount;
    }

    /**
     * 获取总获赞数。
     *
     * @return 总获赞数
     */
    public long getTotalLikeCount() {
        return totalLikeCount;
    }

    /**
     * 设置总获赞数。
     *
     * @param totalLikeCount 总获赞数
     */
    public void setTotalLikeCount(long totalLikeCount) {
        this.totalLikeCount = totalLikeCount;
    }

    /**
     * 获取总收藏数。
     *
     * @return 总收藏数
     */
    public long getTotalFavoriteCount() {
        return totalFavoriteCount;
    }

    /**
     * 设置总收藏数。
     *
     * @param totalFavoriteCount 总收藏数
     */
    public void setTotalFavoriteCount(long totalFavoriteCount) {
        this.totalFavoriteCount = totalFavoriteCount;
    }

    /**
     * 获取总评论数。
     *
     * @return 总评论数
     */
    public long getTotalCommentCount() {
        return totalCommentCount;
    }

    /**
     * 设置总评论数。
     *
     * @param totalCommentCount 总评论数
     */
    public void setTotalCommentCount(long totalCommentCount) {
        this.totalCommentCount = totalCommentCount;
    }

    /**
     * 获取粉丝数。
     *
     * @return 粉丝数
     */
    public int getFollowerCount() {
        return followerCount;
    }

    /**
     * 设置粉丝数。
     *
     * @param followerCount 粉丝数
     */
    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * 获取最新文章 ID。
     *
     * @return 最新文章 ID
     */
    public Long getLatestArticleId() {
        return latestArticleId;
    }

    /**
     * 设置最新文章 ID。
     *
     * @param latestArticleId 最新文章 ID
     */
    public void setLatestArticleId(Long latestArticleId) {
        this.latestArticleId = latestArticleId;
    }

    /**
     * 获取最新文章标题。
     *
     * @return 最新文章标题
     */
    public String getLatestArticleTitle() {
        return latestArticleTitle;
    }

    /**
     * 设置最新文章标题。
     *
     * @param latestArticleTitle 最新文章标题
     */
    public void setLatestArticleTitle(String latestArticleTitle) {
        this.latestArticleTitle = latestArticleTitle;
    }

    /**
     * 获取最新文章状态。
     *
     * @return 最新文章状态
     */
    public String getLatestArticleStatus() {
        return latestArticleStatus;
    }

    /**
     * 设置最新文章状态。
     *
     * @param latestArticleStatus 最新文章状态
     */
    public void setLatestArticleStatus(String latestArticleStatus) {
        this.latestArticleStatus = latestArticleStatus;
    }

    /**
     * 获取最新更新时间。
     *
     * @return 最新更新时间
     */
    public String getLatestUpdatedAt() {
        return latestUpdatedAt;
    }

    /**
     * 设置最新更新时间。
     *
     * @param latestUpdatedAt 最新更新时间
     */
    public void setLatestUpdatedAt(String latestUpdatedAt) {
        this.latestUpdatedAt = latestUpdatedAt;
    }

    /**
     * 获取推荐操作类型。
     *
     * @return 推荐操作类型
     */
    public String getRecommendedActionType() {
        return recommendedActionType;
    }

    /**
     * 设置推荐操作类型。
     *
     * @param recommendedActionType 推荐操作类型
     */
    public void setRecommendedActionType(String recommendedActionType) {
        this.recommendedActionType = recommendedActionType;
    }

    /**
     * 获取推荐操作文案。
     *
     * @return 推荐操作文案
     */
    public String getRecommendedActionText() {
        return recommendedActionText;
    }

    /**
     * 设置推荐操作文案。
     *
     * @param recommendedActionText 推荐操作文案
     */
    public void setRecommendedActionText(String recommendedActionText) {
        this.recommendedActionText = recommendedActionText;
    }

    /**
     * 获取推荐操作提示。
     *
     * @return 推荐操作提示
     */
    public String getRecommendedActionHint() {
        return recommendedActionHint;
    }

    /**
     * 设置推荐操作提示。
     *
     * @param recommendedActionHint 推荐操作提示
     */
    public void setRecommendedActionHint(String recommendedActionHint) {
        this.recommendedActionHint = recommendedActionHint;
    }

    /**
     * 获取推荐操作路由。
     *
     * @return 推荐操作路由
     */
    public String getRecommendedActionRoute() {
        return recommendedActionRoute;
    }

    /**
     * 设置推荐操作路由。
     *
     * @param recommendedActionRoute 推荐操作路由
     */
    public void setRecommendedActionRoute(String recommendedActionRoute) {
        this.recommendedActionRoute = recommendedActionRoute;
    }
}
