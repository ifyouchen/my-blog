package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户学习进度数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class LearningProgressDO {

    /**
     * 进度记录 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 学习资产类型（如：TOPIC 专题、COLUMN 专栏）
     */
    private String assetType;

    /**
     * 学习资产 ID（专题或专栏的 ID）
     */
    private Long assetId;

    /**
     * 已完成文章 ID 列表（JSON 格式存储）
     */
    private String completedArticleIds;

    /**
     * 已完成文章数量
     */
    private Integer completedCount;

    /**
     * 学习进度百分比（0-100）
     */
    private Integer progressPercent;

    /**
     * 最后阅读的文章 ID
     */
    private Long lastArticleId;

    /**
     * 最后阅读时间
     */
    private LocalDateTime lastReadAt;

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
     * 获取进度记录 ID。
     *
     * @return 进度记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置进度记录 ID。
     *
     * @param id 进度记录 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取学习资产类型。
     *
     * @return 资产类型
     */
    public String getAssetType() {
        return assetType;
    }

    /**
     * 设置学习资产类型。
     *
     * @param assetType 资产类型
     */
    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    /**
     * 获取学习资产 ID。
     *
     * @return 资产 ID
     */
    public Long getAssetId() {
        return assetId;
    }

    /**
     * 设置学习资产 ID。
     *
     * @param assetId 资产 ID
     */
    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    /**
     * 获取已完成文章 ID 列表（JSON 格式）。
     *
     * @return 已完成文章 ID 列表
     */
    public String getCompletedArticleIds() {
        return completedArticleIds;
    }

    /**
     * 设置已完成文章 ID 列表（JSON 格式）。
     *
     * @param completedArticleIds 已完成文章 ID 列表
     */
    public void setCompletedArticleIds(String completedArticleIds) {
        this.completedArticleIds = completedArticleIds;
    }

    /**
     * 获取已完成文章数量。
     *
     * @return 已完成数量
     */
    public Integer getCompletedCount() {
        return completedCount;
    }

    /**
     * 设置已完成文章数量。
     *
     * @param completedCount 已完成数量
     */
    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    /**
     * 获取学习进度百分比。
     *
     * @return 进度百分比（0-100）
     */
    public Integer getProgressPercent() {
        return progressPercent;
    }

    /**
     * 设置学习进度百分比。
     *
     * @param progressPercent 进度百分比
     */
    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * 获取最后阅读的文章 ID。
     *
     * @return 文章 ID
     */
    public Long getLastArticleId() {
        return lastArticleId;
    }

    /**
     * 设置最后阅读的文章 ID。
     *
     * @param lastArticleId 文章 ID
     */
    public void setLastArticleId(Long lastArticleId) {
        this.lastArticleId = lastArticleId;
    }

    /**
     * 获取最后阅读时间。
     *
     * @return 最后阅读时间
     */
    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    /**
     * 设置最后阅读时间。
     *
     * @param lastReadAt 最后阅读时间
     */
    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
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
