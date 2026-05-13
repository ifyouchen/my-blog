package com.myblog.interfaces.rest.dto.response;

/**
 * 作者排行榜响应.
 * <p>
 * 用于首页展示活跃作者排行，包含作者基本信息和统计数据.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class AuthorRankingResponse {

    /** 排名序号. */
    private int rank;

    /** 作者用户信息. */
    private UserResponse user;

    /** 发布文章数. */
    private long articleCount;

    /** 总阅读量. */
    private long totalViewCount;

    /** 总点赞数. */
    private long totalLikeCount;

    /** 粉丝数量. */
    private long followerCount;

    /** 当前登录用户是否已关注该作者. */
    private boolean followed;

    /** 代表作（最受欢迎的文章摘要）. */
    private ArticleSummaryResponse topArticle;

    /**
     * 获取排名序号.
     *
     * @return 排名
     */
    public int getRank() {
        return rank;
    }

    /**
     * 设置排名序号.
     *
     * @param rank 排名
     */
    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * 获取作者用户信息.
     *
     * @return 用户信息
     */
    public UserResponse getUser() {
        return user;
    }

    /**
     * 设置作者用户信息.
     *
     * @param user 用户信息
     */
    public void setUser(UserResponse user) {
        this.user = user;
    }

    /**
     * 获取发布文章数.
     *
     * @return 文章数
     */
    public long getArticleCount() {
        return articleCount;
    }

    /**
     * 设置发布文章数.
     *
     * @param articleCount 文章数
     */
    public void setArticleCount(long articleCount) {
        this.articleCount = articleCount;
    }

    /**
     * 获取总阅读量.
     *
     * @return 总阅读量
     */
    public long getTotalViewCount() {
        return totalViewCount;
    }

    /**
     * 设置总阅读量.
     *
     * @param totalViewCount 总阅读量
     */
    public void setTotalViewCount(long totalViewCount) {
        this.totalViewCount = totalViewCount;
    }

    /**
     * 获取总点赞数.
     *
     * @return 总点赞数
     */
    public long getTotalLikeCount() {
        return totalLikeCount;
    }

    /**
     * 设置总点赞数.
     *
     * @param totalLikeCount 总点赞数
     */
    public void setTotalLikeCount(long totalLikeCount) {
        this.totalLikeCount = totalLikeCount;
    }

    /**
     * 获取粉丝数量.
     *
     * @return 粉丝数
     */
    public long getFollowerCount() {
        return followerCount;
    }

    /**
     * 设置粉丝数量.
     *
     * @param followerCount 粉丝数
     */
    public void setFollowerCount(long followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * 当前用户是否已关注该作者.
     *
     * @return 是否已关注
     */
    public boolean isFollowed() {
        return followed;
    }

    /**
     * 设置是否已关注.
     *
     * @param followed 是否已关注
     */
    public void setFollowed(boolean followed) {
        this.followed = followed;
    }

    /**
     * 获取代表作.
     *
     * @return 代表作摘要
     */
    public ArticleSummaryResponse getTopArticle() {
        return topArticle;
    }

    /**
     * 设置代表作.
     *
     * @param topArticle 代表作摘要
     */
    public void setTopArticle(ArticleSummaryResponse topArticle) {
        this.topArticle = topArticle;
    }
}
