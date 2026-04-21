package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.CategoryId;

import java.time.LocalDateTime;

public class Category {

    private CategoryId id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private Category() {
    }

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

    public void update(String name, String description, Integer sortOrder, Boolean enabled) {
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public CategoryId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
