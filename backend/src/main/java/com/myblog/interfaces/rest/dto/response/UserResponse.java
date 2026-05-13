package com.myblog.interfaces.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 用户响应.
 * <p>
 * 用户基本信息，用于API返回中的用户数据展示，null字段不参与序列化.
 *
 * @author my-blog
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    /** 用户ID. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 用户名. */
    private String username;

    /** 邮箱地址. */
    private String email;

    /** 昵称. */
    private String nickname;

    /** 头像URL. */
    private String avatarUrl;

    /** 个人简介. */
    private String bio;

    /** 个人网站. */
    private String website;

    /** GitHub用户名. */
    private String github;

    /** Twitter用户名. */
    private String twitter;

    /** 所在地. */
    private String location;

    /** 最后登录时间（ISO格式）. */
    private String lastLoginAt;

    /** 最后登录IP. */
    private String lastLoginIp;

    /** 用户角色（如 USER、ADMIN）. */
    private String role;

    /** 账号状态（如 ACTIVE、BANNED）. */
    private String status;

    /** 当前登录用户是否已关注此用户. */
    private Boolean followed;

    /** 粉丝数量. */
    private long followerCount;

    /** 发布文章数. */
    private int articleCount;

    /** 总获赞数. */
    private long totalLikeCount;

    /**
     * 获取用户ID.
     *
     * @return 用户ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户ID.
     *
     * @param id 用户ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名.
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名.
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取邮箱地址.
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱地址.
     *
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取昵称.
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称.
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取头像URL.
     *
     * @return 头像URL
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 设置头像URL.
     *
     * @param avatarUrl 头像URL
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 获取个人简介.
     *
     * @return 简介
     */
    public String getBio() {
        return bio;
    }

    /**
     * 设置个人简介.
     *
     * @param bio 简介
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * 获取个人网站.
     *
     * @return 网站URL
     */
    public String getWebsite() {
        return website;
    }

    /**
     * 设置个人网站.
     *
     * @param website 网站URL
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * 获取GitHub用户名.
     *
     * @return GitHub用户名
     */
    public String getGithub() {
        return github;
    }

    /**
     * 设置GitHub用户名.
     *
     * @param github GitHub用户名
     */
    public void setGithub(String github) {
        this.github = github;
    }

    /**
     * 获取Twitter用户名.
     *
     * @return Twitter用户名
     */
    public String getTwitter() {
        return twitter;
    }

    /**
     * 设置Twitter用户名.
     *
     * @param twitter Twitter用户名
     */
    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    /**
     * 获取所在地.
     *
     * @return 所在地
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置所在地.
     *
     * @param location 所在地
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 获取最后登录时间.
     *
     * @return 登录时间（ISO格式）
     */
    public String getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * 设置最后登录时间.
     *
     * @param lastLoginAt 登录时间（ISO格式）
     */
    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * 获取最后登录IP.
     *
     * @return 登录IP
     */
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /**
     * 设置最后登录IP.
     *
     * @param lastLoginIp 登录IP
     */
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    /**
     * 获取用户角色.
     *
     * @return 角色名称
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置用户角色.
     *
     * @param role 角色名称
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取账号状态.
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置账号状态.
     *
     * @param status 状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 当前登录用户是否已关注此用户.
     *
     * @return 是否已关注
     */
    public Boolean getFollowed() {
        return followed;
    }

    /**
     * 设置当前登录用户是否已关注此用户.
     *
     * @param followed 是否已关注
     */
    public void setFollowed(Boolean followed) {
        this.followed = followed;
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
     * 获取发布文章数.
     *
     * @return 文章数
     */
    public int getArticleCount() {
        return articleCount;
    }

    /**
     * 设置发布文章数.
     *
     * @param articleCount 文章数
     */
    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
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
}
