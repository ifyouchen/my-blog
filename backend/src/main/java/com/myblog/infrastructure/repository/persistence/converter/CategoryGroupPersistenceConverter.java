package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.CategoryGroup;
import com.myblog.infrastructure.repository.persistence.entity.CategoryGroupDO;

/**
 * 文章分类组持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CategoryGroupPersistenceConverter {

    private CategoryGroupPersistenceConverter() {
    }

    /**
     * 将分类组数据对象转换为领域对象。
     *
     * @param groupDO 分类组数据对象
     * @return 分类组领域对象
     */
    public static CategoryGroup toDomain(CategoryGroupDO groupDO) {
        if (groupDO == null) {
            return null;
        }
        return CategoryGroup.restore(
            groupDO.getId(),
            groupDO.getName(),
            groupDO.getDescription(),
            groupDO.getSortOrder(),
            groupDO.getEnabled(),
            groupDO.getCreatedAt(),
            groupDO.getUpdatedAt(),
            groupDO.getDeletedAt(),
            groupDO.getVersion()
        );
    }

    /**
     * 将分类组领域对象转换为数据对象。
     *
     * @param group 分类组领域对象
     * @return 分类组数据对象
     */
    public static CategoryGroupDO toData(CategoryGroup group) {
        if (group == null) {
            return null;
        }
        CategoryGroupDO groupDO = new CategoryGroupDO();
        groupDO.setId(group.getId().getValue());
        groupDO.setName(group.getName());
        groupDO.setDescription(group.getDescription());
        groupDO.setSortOrder(group.getSortOrder());
        groupDO.setEnabled(group.getEnabled());
        groupDO.setCreatedAt(group.getCreatedAt());
        groupDO.setUpdatedAt(group.getUpdatedAt());
        groupDO.setDeletedAt(group.getDeletedAt());
        groupDO.setVersion(group.getVersion());
        return groupDO;
    }
}
