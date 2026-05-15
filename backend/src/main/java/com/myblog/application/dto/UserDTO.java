package com.myblog.application.dto;

/**
 * 用户应用数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String website;
    private String github;
    private String twitter;
    private String location;
    private String lastLoginAt;
    private String lastLoginIp;
    private String role;
    private String status;
    private Boolean followed;
    private long followerCount;
    private int articleCount;
    private long totalLikeCount;
    private int currentLevel;

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
     * 获取邮箱。
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
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
     * 获取个人网站。
     *
     * @return 个人网站
     */
    public String getWebsite() {
        return website;
    }

    /**
     * 设置个人网站。
     *
     * @param website 个人网站
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * 获取 GitHub 主页。
     *
     * @return GitHub 主页
     */
    public String getGithub() {
        return github;
    }

    /**
     * 设置 GitHub 主页。
     *
     * @param github GitHub 主页
     */
    public void setGithub(String github) {
        this.github = github;
    }

    /**
     * 获取 Twitter 主页。
     *
     * @return Twitter 主页
     */
    public String getTwitter() {
        return twitter;
    }

    /**
     * 设置 Twitter 主页。
     *
     * @param twitter Twitter 主页
     */
    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    /**
     * 获取所在地。
     *
     * @return 所在地
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置所在地。
     *
     * @param location 所在地
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 获取最后登录时间。
     *
     * @return 最后登录时间
     */
    public String getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * 设置最后登录时间。
     *
     * @param lastLoginAt 最后登录时间
     */
    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * 获取最后登录 IP。
     *
     * @return 最后登录 IP
     */
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /**
     * 设置最后登录 IP。
     *
     * @param lastLoginIp 最后登录 IP
     */
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    /**
     * 获取用户角色。
     *
     * @return 用户角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置用户角色。
     *
     * @param role 用户角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取用户状态。
     *
     * @return 用户状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置用户状态。
     *
     * @param status 用户状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取当前用户是否已关注该作者。
     *
     * @return 是否已关注
     */
    public Boolean getFollowed() {
        return followed;
    }

    /**
     * 设置当前用户是否已关注该作者。
     *
     * @param followed 是否已关注
     */
    public void setFollowed(Boolean followed) {
        this.followed = followed;
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
     * 获取文章数量。
     *
     * @return 文章数量
     */
    public int getArticleCount() {
        return articleCount;
    }

    /**
     * 设置文章数量。
     *
     * @param articleCount 文章数量
     */
    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
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
     * 获取用户当前等级。
     *
     * @return 当前等级
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 设置用户当前等级。
     *
     * @param currentLevel 当前等级
     */
    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }
}
