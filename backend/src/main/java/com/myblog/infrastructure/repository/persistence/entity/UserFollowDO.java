package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户关注数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserFollowDO {

    /**
     * 关注关系 ID
     */
    private Long id;

    /**
     * 发起关注的用户 ID（粉丝）
     */
    private Long followerUserId;

    /**
     * 被关注的用户 ID（博主）
     */
    private Long followingUserId;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 获取关注关系 ID。
     *
     * @return 关注关系 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置关注关系 ID。
     *
     * @param id 关注关系 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取发起关注的用户 ID（粉丝）。
     *
     * @return 粉丝用户 ID
     */
    public Long getFollowerUserId() {
        return followerUserId;
    }

    /**
     * 设置发起关注的用户 ID（粉丝）。
     *
     * @param followerUserId 粉丝用户 ID
     */
    public void setFollowerUserId(Long followerUserId) {
        this.followerUserId = followerUserId;
    }

    /**
     * 获取被关注的用户 ID（博主）。
     *
     * @return 被关注用户 ID
     */
    public Long getFollowingUserId() {
        return followingUserId;
    }

    /**
     * 设置被关注的用户 ID（博主）。
     *
     * @param followingUserId 被关注用户 ID
     */
    public void setFollowingUserId(Long followingUserId) {
        this.followingUserId = followingUserId;
    }

    /**
     * 获取记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置记录创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取记录最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置记录最后更新时间。
     *
     * @param updatedAt 最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置软删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置乐观锁版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
