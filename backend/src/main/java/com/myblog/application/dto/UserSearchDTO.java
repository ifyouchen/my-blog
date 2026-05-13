package com.myblog.application.dto;

/**
 * 用户搜索数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UserSearchDTO {

    /** 用户 ID */
    private Long id;
    /** 用户名 */
    private String username;
    /** 昵称 */
    private String nickname;
    /** 头像地址 */
    private String avatarUrl;
    /** 个人简介 */
    private String bio;
    /** 粉丝数 */
    private Integer followerCount;
    /** 已发布文章数 */
    private Integer publishedArticleCount;
    /** 当前用户是否已关注 */
    private Boolean followed;

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户 ID。
     *
     * @param id 用户 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取昵称。
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称。
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取头像地址。
     *
     * @return 头像地址
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 设置头像地址。
     *
     * @param avatarUrl 头像地址
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 获取个人简介。
     *
     * @return 个人简介
     */
    public String getBio() {
        return bio;
    }

    /**
     * 设置个人简介。
     *
     * @param bio 个人简介
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * 获取粉丝数。
     *
     * @return 粉丝数
     */
    public Integer getFollowerCount() {
        return followerCount;
    }

    /**
     * 设置粉丝数。
     *
     * @param followerCount 粉丝数
     */
    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * 获取已发布文章数。
     *
     * @return 已发布文章数
     */
    public Integer getPublishedArticleCount() {
        return publishedArticleCount;
    }

    /**
     * 设置已发布文章数。
     *
     * @param publishedArticleCount 已发布文章数
     */
    public void setPublishedArticleCount(Integer publishedArticleCount) {
        this.publishedArticleCount = publishedArticleCount;
    }

    /**
     * 获取当前用户是否已关注。
     *
     * @return 是否已关注
     */
    public Boolean getFollowed() {
        return followed;
    }

    /**
     * 设置当前用户是否已关注。
     *
     * @param followed 是否已关注
     */
    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }
}