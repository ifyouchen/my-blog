package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;
import com.myblog.infrastructure.repository.persistence.entity.CategoryDO;

/**
 * 文章分类持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CategoryPersistenceConverter {

    /**
     * 将分类数据对象转换为领域对象。
     *
     * @param categoryDO 分类数据对象
     * @return 分类领域对象，若 categoryDO 为 null 则返回 null
     */
    public static Category toDomain(CategoryDO categoryDO) {
        if (categoryDO == null) {
            return null;
        }
        return Category.restore(
            categoryDO.getId(),
            categoryDO.getName(),
            categoryDO.getGroupId(),
            categoryDO.getGroupName(),
            categoryDO.getDescription(),
            categoryDO.getSortOrder(),
            categoryDO.getEnabled(),
            categoryDO.getCreatedAt(),
            categoryDO.getUpdatedAt(),
            categoryDO.getDeletedAt(),
            categoryDO.getVersion()
        );
    }

    /**
     * 将分类领域对象转换为数据对象。
     *
     * @param category 分类领域对象
     * @return 分类数据对象，若 category 为 null 则返回 null
     */
    public static CategoryDO toData(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDO categoryDO = new CategoryDO();
        categoryDO.setId(category.getId().getValue());
        categoryDO.setName(category.getName());
        categoryDO.setGroupId(category.getGroupId());
        categoryDO.setGroupName(category.getGroupName());
        categoryDO.setDescription(category.getDescription());
        categoryDO.setSortOrder(category.getSortOrder());
        categoryDO.setEnabled(category.getEnabled());
        categoryDO.setCreatedAt(category.getCreatedAt());
        categoryDO.setUpdatedAt(category.getUpdatedAt());
        categoryDO.setDeletedAt(category.getDeletedAt());
        categoryDO.setVersion(category.getVersion());
        return categoryDO;
    }
}
