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

    /**
     * 关注关系 ID
     */
    private UserFollowId id;

    /**
     * 发起关注的用户 ID（粉丝）
     */
    private UserId followerUserId;

    /**
     * 被关注的用户 ID（博主）
     */
    private UserId followingUserId;

    /**
     * 关注创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 关注最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示关注有效
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private UserFollow() {
    }

    /**
     * 创建用户关注聚合根。
     *
     * @param id              关注关系 ID
     * @param followerUserId  发起关注的用户 ID
     * @param followingUserId 被关注的用户 ID
     * @return 用户关注聚合根
     */
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

    /**
     * 从持久化数据还原用户关注聚合根。
     *
     * @param id              关注关系 ID
     * @param followerUserId  发起关注的用户 ID
     * @param followingUserId 被关注的用户 ID
     * @param createdAt       创建时间
     * @param updatedAt       更新时间
     * @param deletedAt       删除时间
     * @param version         乐观锁版本号
     * @return 用户关注聚合根
     */
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

    /**
     * 取消关注（软删除）。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    /**
     * 重新激活已取消的关注。
     */
    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断关注是否已取消。
     *
     * @return 已取消返回 true，否则返回 false
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 获取关注关系 ID。
     *
     * @return 关注关系 ID
     */
    public UserFollowId getId() {
        return id;
    }

    /**
     * 获取发起关注的用户 ID（粉丝）。
     *
     * @return 粉丝用户 ID
     */
    public UserId getFollowerUserId() {
        return followerUserId;
    }

    /**
     * 获取被关注的用户 ID（博主）。
     *
     * @return 被关注用户 ID
     */
    public UserId getFollowingUserId() {
        return followingUserId;
    }

    /**
     * 获取关注创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取关注最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未取消则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
