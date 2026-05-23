package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.CategoryGroup;
import com.myblog.domain.model.valueobject.CategoryGroupId;

import java.util.List;
import java.util.Optional;

/**
 * 分类组仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CategoryGroupRepository {

    /**
     * 根据 ID 查询分类组。
     *
     * @param id 分类组 ID
     * @return 分类组 Optional
     */
    Optional<CategoryGroup> findById(CategoryGroupId id);

    /**
     * 根据名称查询分类组。
     *
     * @param name 分类组名称
     * @return 分类组 Optional
     */
    Optional<CategoryGroup> findByName(String name);

    /**
     * 查询分类组列表。
     *
     * @param enabled 是否启用，null 表示全部
     * @return 分类组列表
     */
    List<CategoryGroup> findAll(Boolean enabled);

    /**
     * 判断分类组名称是否存在。
     *
     * @param name 分类组名称
     * @param excludeId 排除的分类组 ID
     * @return 是否存在
     */
    boolean existsByName(String name, CategoryGroupId excludeId);

    /**
     * 保存分类组。
     *
     * @param group 分类组
     * @return 保存后的分类组
     */
    CategoryGroup save(CategoryGroup group);

    /**
     * 生成下一个分类组 ID。
     *
     * @return 分类组 ID
     */
    Long nextId();
}
