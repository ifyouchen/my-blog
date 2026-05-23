package com.myblog.application.dto;

import java.time.LocalDateTime;

/**
 * 分类数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CategoryDTO {

    /** 分类 ID */
    private Long id;
    /** 分类名称 */
    private String name;
    /** 所属分类组 ID */
    private Long groupId;
    /** 所属大类 */
    private String groupName;
    /** 分类描述 */
    private String description;
    /** 排序值 */
    private Integer sortOrder;
    /** 是否启用 */
    private Boolean enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 获取分类 ID。
     *
     * @return 分类 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置分类 ID。
     *
     * @param id 分类 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取分类名称。
     *
     * @return 分类名称
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取所属分类组 ID。
     *
     * @return 所属分类组 ID
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * 设置所属分类组 ID。
     *
     * @param groupId 所属分类组 ID
     */
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    /**
     * 获取所属大类。
     *
     * @return 所属大类
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 设置所属大类。
     *
     * @param groupName 所属大类
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 设置分类描述。
     *
     * @param description 分类描述
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
