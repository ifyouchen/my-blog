package com.myblog.application.dto;

/**
 * 用户搜索数据传输对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserSearchDTO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer followerCount;
    private Integer publishedArticleCount;
    private Boolean followed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
    }

    public Integer getPublishedArticleCount() {
        return publishedArticleCount;
    }

    public void setPublishedArticleCount(Integer publishedArticleCount) {
        this.publishedArticleCount = publishedArticleCount;
    }

    public Boolean getFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }
}