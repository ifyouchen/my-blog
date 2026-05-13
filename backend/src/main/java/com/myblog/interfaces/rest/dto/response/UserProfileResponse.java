package com.myblog.interfaces.rest.dto.response;

/**
 * 用户主页响应.
 * <p>
 * 用户个人主页展示的聚合数据，包含用户信息及各项统计数据.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UserProfileResponse {

    /** 用户基本信息. */
    private UserResponse user;

    /** 发布文章总数. */
    private long articleCount;

    /** 总阅读量. */
    private long totalViewCount;

    /** 总获赞数. */
    private long totalLikeCount;

    /** 粉丝数量. */
    private long followerCount;

    /** 关注数量. */
    private long followingCount;

    /** 当前登录用户是否已关注此用户. */
    private boolean following;

    /**
     * 获取用户基本信息.
     *
     * @return 用户信息
     */
    public UserResponse getUser() {
        return user;
    }

    /**
     * 设置用户基本信息.
     *
     * @param user 用户信息
     */
    public void setUser(UserResponse user) {
        this.user = user;
    }

    /**
     * 获取发布文章总数.
     *
     * @return 文章数
     */
    public long getArticleCount() {
        return articleCount;
    }

    /**
     * 设置发布文章总数.
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
     * 获取总获赞数.
     *
     * @return 总获赞数
     */
    public long getTotalLikeCount() {
        return totalLikeCount;
    }

    /**
     * 设置总获赞数.
     *
     * @param totalLikeCount 总获赞数
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
     * 获取关注数量.
     *
     * @return 关注数
     */
    public long getFollowingCount() {
        return followingCount;
    }

    /**
     * 设置关注数量.
     *
     * @param followingCount 关注数
     */
    public void setFollowingCount(long followingCount) {
        this.followingCount = followingCount;
    }

    /**
     * 当前登录用户是否已关注此用户.
     *
     * @return 是否已关注
     */
    public boolean isFollowing() {
        return following;
    }

    /**
     * 设置当前登录用户是否已关注此用户.
     *
     * @param following 是否已关注
     */
    public void setFollowing(boolean following) {
        this.following = following;
    }
}
