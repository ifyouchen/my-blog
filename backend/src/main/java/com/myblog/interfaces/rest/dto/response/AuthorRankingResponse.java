package com.myblog.interfaces.rest.dto.response;

/**
 * 作者排行榜响应。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthorRankingResponse {

    private int rank;
    private UserResponse user;
    private long articleCount;
    private long totalViewCount;
    private long totalLikeCount;
    private long followerCount;
    private boolean followed;
    private ArticleSummaryResponse topArticle;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
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

    public ArticleSummaryResponse getTopArticle() {
        return topArticle;
    }

    public void setTopArticle(ArticleSummaryResponse topArticle) {
        this.topArticle = topArticle;
    }
}
