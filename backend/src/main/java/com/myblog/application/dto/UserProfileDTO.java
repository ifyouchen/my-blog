package com.myblog.application.dto;

/**
 * 用户主页 DTO。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserProfileDTO {

    private UserDTO user;
    private long articleCount;
    private long totalViewCount;
    private long totalLikeCount;

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
     * 获取总点赞量。
     *
     * @return 总点赞量
     */
    public long getTotalLikeCount() {
        return totalLikeCount;
    }

    /**
     * 设置总点赞量。
     *
     * @param totalLikeCount 总点赞量
     */
    public void setTotalLikeCount(long totalLikeCount) {
        this.totalLikeCount = totalLikeCount;
    }
}
