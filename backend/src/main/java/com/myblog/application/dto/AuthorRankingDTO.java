package com.myblog.application.dto;

/**
 * 作者排行榜应用数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthorRankingDTO {

    private int rank;
    private UserDTO user;
    private long articleCount;
    private long totalViewCount;
    private long totalLikeCount;
    private long followerCount;
    private boolean followed;
    private ArticleSummaryDTO topArticle;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public long getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(long articleCount) {
        this.articleCount = articleCount;
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

    public long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(long followerCount) {
        this.followerCount = followerCount;
    }

    public boolean isFollowed() {
        return followed;
    }

    public void setFollowed(boolean followed) {
        this.followed = followed;
    }

    public ArticleSummaryDTO getTopArticle() {
        return topArticle;
    }

    public void setTopArticle(ArticleSummaryDTO topArticle) {
        this.topArticle = topArticle;
    }
}
