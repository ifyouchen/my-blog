package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 专栏订阅数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ColumnSubscriptionDO {

    /**
     * 订阅记录 ID
     */
    private Long id;

    /**
     * 被订阅的专栏 ID
     */
    private Long columnId;

    /**
     * 订阅用户 ID
     */
    private Long userId;

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
     * 获取订阅记录 ID。
     *
     * @return 订阅记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置订阅记录 ID。
     *
     * @param id 订阅记录 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取被订阅的专栏 ID。
     *
     * @return 专栏 ID
     */
    public Long getColumnId() {
        return columnId;
    }

    /**
     * 设置被订阅的专栏 ID。
     *
     * @param columnId 专栏 ID
     */
    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }

    /**
     * 获取订阅用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置订阅用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
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
