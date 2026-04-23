package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.UserFollowId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 用户关注聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserFollow {

    private UserFollowId id;
    private UserId followerUserId;
    private UserId followingUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private UserFollow() {
    }

    public static UserFollow create(Long id, UserId followerUserId, UserId followingUserId) {
        UserFollow follow = new UserFollow();
        follow.id = new UserFollowId(id);
        follow.followerUserId = followerUserId;
        follow.followingUserId = followingUserId;
        follow.createdAt = LocalDateTime.now();
        follow.updatedAt = follow.createdAt;
        follow.deletedAt = null;
        follow.version = 0;
        return follow;
    }

    public static UserFollow restore(Long id, UserId followerUserId, UserId followingUserId,
                                     LocalDateTime createdAt, LocalDateTime updatedAt,
                                     LocalDateTime deletedAt, Integer version) {
        UserFollow follow = new UserFollow();
        follow.id = new UserFollowId(id);
        follow.followerUserId = followerUserId;
        follow.followingUserId = followingUserId;
        follow.createdAt = createdAt;
        follow.updatedAt = updatedAt;
        follow.deletedAt = deletedAt;
        follow.version = version;
        return follow;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UserFollowId getId() {
        return id;
    }

    public UserId getFollowerUserId() {
        return followerUserId;
    }

    public UserId getFollowingUserId() {
        return followingUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
