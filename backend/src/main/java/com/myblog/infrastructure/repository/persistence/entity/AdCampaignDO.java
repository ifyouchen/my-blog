package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 广告投放 MyBatis DO 实体。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdCampaignDO {

    /**
     * 广告投放 ID
     */
    private Long id;

    /**
     * 广告位编码
     */
    private String slotCode;

    /**
     * 广告标题
     */
    private String title;

    /**
     * 广告图片 URL
     */
    private String imageUrl;

    /**
     * 广告跳转目标 URL
     */
    private String targetUrl;

    /**
     * 广告标签（如：推广、赞助）
     */
    private String label;

    /**
     * 广告投放开始时间
     */
    private LocalDateTime startAt;

    /**
     * 广告投放结束时间
     */
    private LocalDateTime endAt;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序权重，值越小越靠前
     */
    private Integer sortOrder;

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
     * 获取广告投放 ID。
     *
     * @return 广告投放 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置广告投放 ID。
     *
     * @param id 广告投放 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取广告位编码。
     *
     * @return 广告位编码
     */
    public String getSlotCode() {
        return slotCode;
    }

    /**
     * 设置广告位编码。
     *
     * @param slotCode 广告位编码
     */
    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    /**
     * 获取广告标题。
     *
     * @return 广告标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置广告标题。
     *
     * @param title 广告标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取广告图片 URL。
     *
     * @return 图片 URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 设置广告图片 URL。
     *
     * @param imageUrl 图片 URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 获取广告跳转目标 URL。
     *
     * @return 目标 URL
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * 设置广告跳转目标 URL。
     *
     * @param targetUrl 目标 URL
     */
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * 获取广告标签。
     *
     * @return 广告标签
     */
    public String getLabel() {
        return label;
    }

    /**
     * 设置广告标签。
     *
     * @param label 广告标签
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * 获取广告投放开始时间。
     *
     * @return 开始时间
     */
    public LocalDateTime getStartAt() {
        return startAt;
    }

    /**
     * 设置广告投放开始时间。
     *
     * @param startAt 开始时间
     */
    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    /**
     * 获取广告投放结束时间。
     *
     * @return 结束时间
     */
    public LocalDateTime getEndAt() {
        return endAt;
    }

    /**
     * 设置广告投放结束时间。
     *
     * @param endAt 结束时间
     */
    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    /**
     * 获取是否启用。
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取排序权重。
     *
     * @return 排序权重
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置排序权重。
     *
     * @param sortOrder 排序权重
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

