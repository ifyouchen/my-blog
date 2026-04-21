package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;
import com.myblog.infrastructure.repository.persistence.entity.CategoryDO;

public class CategoryPersistenceConverter {

    public static Category toDomain(CategoryDO categoryDO) {
        if (categoryDO == null) {
            return null;
        }
        return Category.restore(
            categoryDO.getId(),
            categoryDO.getName(),
            categoryDO.getDescription(),
            categoryDO.getSortOrder(),
            categoryDO.getEnabled(),
            categoryDO.getCreatedAt(),
            categoryDO.getUpdatedAt(),
            categoryDO.getDeletedAt(),
            categoryDO.getVersion()
        );
    }

    public static CategoryDO toData(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDO categoryDO = new CategoryDO();
        categoryDO.setId(category.getId().getValue());
        categoryDO.setName(category.getName());
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