package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户关注数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserFollowDO {

    private Long id;
    private Long followerUserId;
    private Long followingUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFollowerUserId() {
        return followerUserId;
    }

    public void setFollowerUserId(Long followerUserId) {
        this.followerUserId = followerUserId;
    }

    public Long getFollowingUserId() {
        return followingUserId;
    }

    public void setFollowingUserId(Long followingUserId) {
        this.followingUserId = followingUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
