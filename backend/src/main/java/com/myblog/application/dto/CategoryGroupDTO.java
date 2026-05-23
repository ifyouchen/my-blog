package com.myblog.application.dto;

import java.time.LocalDateTime;

/**
 * 分类组数据传输对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CategoryGroupDTO {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private Long categoryCount;
    private String categoryNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取分类组 ID。
     *
     * @return 分类组 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置分类组 ID。
     *
     * @param id 分类组 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取分类组名称。
     *
     * @return 分类组名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置分类组名称。
     *
     * @param name 分类组名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取分类组说明。
     *
     * @return 分类组说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置分类组说明。
     *
     * @param description 分类组说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取排序值。
     *
     * @return 排序值
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置排序值。
     *
     * @param sortOrder 排序值
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
     * 获取小分类数量。
     *
     * @return 小分类数量
     */
    public Long getCategoryCount() {
        return categoryCount;
    }

    /**
     * 设置小分类数量。
     *
     * @param categoryCount 小分类数量
     */
    public void setCategoryCount(Long categoryCount) {
        this.categoryCount = categoryCount;
    }

    /**
     * 获取小分类名称预览。
     *
     * @return 小分类名称预览
     */
    public String getCategoryNames() {
        return categoryNames;
    }

    /**
     * 设置小分类名称预览。
     *
     * @param categoryNames 小分类名称预览
     */
    public void setCategoryNames(String categoryNames) {
        this.categoryNames = categoryNames;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
