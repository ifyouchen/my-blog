package com.myblog.application.dto;

/**
 * 作者排行榜应用数据。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class AuthorRankingDTO {

    /** 排名 */
    private int rank;
    /** 用户信息 */
    private UserDTO user;
    /** 文章数量 */
    private long articleCount;
    /** 总阅读量 */
    private long totalViewCount;
    /** 总获赞数 */
    private long totalLikeCount;
    /** 粉丝数 */
    private long followerCount;
    /** 当前用户是否已关注 */
    private boolean followed;
    /** 代表作 */
    private ArticleSummaryDTO topArticle;

    /**
     * 获取排名。
     *
     * @return 排名
     */
    public int getRank() {
        return rank;
    }

    /**
     * 设置排名。
     *
     * @param rank 排名
     */
    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * 获取用户信息。
     *
     * @return 用户信息
     */
    public UserDTO getUser() {
        return user;
    }

    /**
     * 设置用户信息。
     *
     * @param user 用户信息
     */
    public void setUser(UserDTO user) {
        this.user = user;
    }

    /**
     * 获取文章数量。
     *
     * @return 文章数量
     */
    public long getArticleCount() {
        return articleCount;
    }

    /**
     * 设置文章数量。
     *
     * @param articleCount 文章数量
     */
    public void setArticleCount(long articleCount) {
        this.articleCount = articleCount;
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
     * 获取粉丝数。
     *
     * @return 粉丝数
     */
    public long getFollowerCount() {
        return followerCount;
    }

    /**
     * 设置粉丝数。
     *
     * @param followerCount 粉丝数
     */
    public void setFollowerCount(long followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * 获取当前用户是否已关注。
     *
     * @return 是否已关注
     */
    public boolean isFollowed() {
        return followed;
    }

    /**
     * 设置当前用户是否已关注。
     *
     * @param followed 是否已关注
     */
    public void setFollowed(boolean followed) {
        this.followed = followed;
    }

    /**
     * 获取代表作。
     *
     * @return 代表作
     */
    public ArticleSummaryDTO getTopArticle() {
        return topArticle;
    }

    /**
     * 设置代表作。
     *
     * @param topArticle 代表作
     */
    public void setTopArticle(ArticleSummaryDTO topArticle) {
        this.topArticle = topArticle;
    }
}
