package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 公告持久化对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AnnouncementDO {

    /**
     * 公告 ID
     */
    private Long id;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 公告目标受众（如：ALL、USER、ADMIN）
     */
    private String target;

    /**
     * 是否已发布（1：已发布，0：未发布）
     */
    private Integer published;

    /**
     * 公告发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 公告过期时间，为 null 表示永不过期
     */
    private LocalDateTime expiresAt;

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
     * 获取公告 ID。
     *
     * @return 公告 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置公告 ID。
     *
     * @param id 公告 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取公告标题。
     *
     * @return 公告标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置公告标题。
     *
     * @param title 公告标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取公告内容。
     *
     * @return 公告内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置公告内容。
     *
     * @param content 公告内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取公告目标受众。
     *
     * @return 目标受众
     */
    public String getTarget() {
        return target;
    }

    /**
     * 设置公告目标受众。
     *
     * @param target 目标受众
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * 获取是否已发布。
     *
     * @return 1 表示已发布，0 表示未发布
     */
    public Integer getPublished() {
        return published;
    }

    /**
     * 设置是否已发布。
     *
     * @param published 发布状态（1：已发布，0：未发布）
     */
    public void setPublished(Integer published) {
        this.published = published;
    }

    /**
     * 获取公告发布时间。
     *
     * @return 发布时间
     */
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    /**
     * 设置公告发布时间。
     *
     * @param publishedAt 发布时间
     */
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    /**
     * 获取公告过期时间。
     *
     * @return 过期时间，为 null 表示永不过期
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 设置公告过期时间。
     *
     * @param expiresAt 过期时间
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

