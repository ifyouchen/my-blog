package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.CategoryId;

import java.time.LocalDateTime;

/**
 * 文章分类聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class Category {

    /**
     * 分类 ID
     */
    private CategoryId id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重，值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 分类创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 分类最后更新时间
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

    private Category() {
    }

    /**
     * 创建分类聚合根。
     *
     * @param id          分类 ID
     * @param name        分类名称
     * @param description 分类描述
     * @param sortOrder   排序权重
     * @return 分类聚合根
     */
    public static Category create(Long id, String name, String description, Integer sortOrder) {
        Category category = new Category();
        category.id = new CategoryId(id);
        category.name = name;
        category.description = description;
        category.sortOrder = sortOrder;
        category.enabled = true;
        category.createdAt = LocalDateTime.now();
        category.updatedAt = category.createdAt;
        category.deletedAt = null;
        category.version = 0;
        return category;
    }

    /**
     * 从持久化数据还原分类聚合根。
     *
     * @param id          分类 ID
     * @param name        分类名称
     * @param description 分类描述
     * @param sortOrder   排序权重
     * @param enabled     是否启用
     * @param createdAt   创建时间
     * @param updatedAt   更新时间
     * @param deletedAt   删除时间
     * @param version     乐观锁版本号
     * @return 分类聚合根
     */
    public static Category restore(Long id, String name, String description, Integer sortOrder,
                                    Boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt,
                                    LocalDateTime deletedAt, Integer version) {
        Category category = new Category();
        category.id = new CategoryId(id);
        category.name = name;
        category.description = description;
        category.sortOrder = sortOrder;
        category.enabled = enabled;
        category.createdAt = createdAt;
        category.updatedAt = updatedAt;
        category.deletedAt = deletedAt;
        category.version = version;
        return category;
    }

    /**
     * 更新分类信息。
     *
     * @param name        新分类名称
     * @param description 新分类描述
     * @param sortOrder   新排序权重
     * @param enabled     是否启用
     */
    public void update(String name, String description, Integer sortOrder, Boolean enabled) {
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    /**
     * 软删除分类。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    /**
     * 判断分类是否已删除。
     *
     * @return 已删除返回 true，否则返回 false
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 获取分类 ID。
     *
     * @return 分类 ID
     */
    public CategoryId getId() {
        return id;
    }

    /**
     * 获取分类名称。
     *
     * @return 分类名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取分类描述。
     *
     * @return 分类描述
     */
    public String getDescription() {
        return description;
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
     * 获取是否启用。
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 获取分类创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取分类最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
