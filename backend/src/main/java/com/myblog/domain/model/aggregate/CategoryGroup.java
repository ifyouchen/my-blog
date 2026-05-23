package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.CategoryGroupId;

import java.time.LocalDateTime;

/**
 * 文章分类组聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CategoryGroup {

    private CategoryGroupId id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private CategoryGroup() {
    }

    /**
     * 创建分类组聚合根。
     *
     * @param id 分类组 ID
     * @param name 分类组名称
     * @param description 分类组说明
     * @param sortOrder 排序值
     * @return 分类组聚合根
     */
    public static CategoryGroup create(Long id, String name, String description, Integer sortOrder) {
        CategoryGroup group = new CategoryGroup();
        group.id = new CategoryGroupId(id);
        group.name = name;
        group.description = description;
        group.sortOrder = sortOrder == null ? 0 : sortOrder;
        group.enabled = true;
        group.createdAt = LocalDateTime.now();
        group.updatedAt = group.createdAt;
        group.deletedAt = null;
        group.version = 0;
        return group;
    }

    /**
     * 从持久化数据还原分类组聚合根。
     *
     * @param id 分类组 ID
     * @param name 分类组名称
     * @param description 分类组说明
     * @param sortOrder 排序值
     * @param enabled 是否启用
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param deletedAt 删除时间
     * @param version 版本号
     * @return 分类组聚合根
     */
    public static CategoryGroup restore(Long id, String name, String description, Integer sortOrder,
                                        Boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt,
                                        LocalDateTime deletedAt, Integer version) {
        CategoryGroup group = new CategoryGroup();
        group.id = new CategoryGroupId(id);
        group.name = name;
        group.description = description;
        group.sortOrder = sortOrder;
        group.enabled = enabled;
        group.createdAt = createdAt;
        group.updatedAt = updatedAt;
        group.deletedAt = deletedAt;
        group.version = version;
        return group;
    }

    /**
     * 更新分类组信息。
     *
     * @param name 分类组名称
     * @param description 分类组说明
     * @param sortOrder 排序值
     * @param enabled 是否启用
     */
    public void update(String name, String description, Integer sortOrder, Boolean enabled) {
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.enabled = enabled == null ? Boolean.TRUE : enabled;
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    /**
     * 软删除分类组。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    /**
     * 获取分类组 ID。
     *
     * @return 分类组 ID
     */
    public CategoryGroupId getId() {
        return id;
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
     * 获取分类组说明。
     *
     * @return 分类组说明
     */
    public String getDescription() {
        return description;
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
     * 获取是否启用。
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
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
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取删除时间。
     *
     * @return 删除时间
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
