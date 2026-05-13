package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;

import java.util.List;
import java.util.Optional;

/**
 * 分类仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface CategoryRepository {

    /**
     * 根据分类 ID 查询分类。
     *
     * @param categoryId 分类 ID
     * @return 分类 Optional
     */
    Optional<Category> findById(CategoryId categoryId);

    /**
     * 查询所有分类，可按启用状态筛选。
     *
     * @param enabled 是否启用，为 null 时不筛选
     * @return 分类列表
     */
    List<Category> findAll(Boolean enabled);

    /**
     * 分页查询分类。
     *
     * @param enabled  是否启用，为 null 时不筛选
     * @param page     页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 分类列表
     */
    List<Category> findPage(Boolean enabled, int page, int pageSize);

    /**
     * 统计分类数量。
     *
     * @param enabled 是否启用，为 null 时不筛选
     * @return 分类数量
     */
    long count(Boolean enabled);

    /**
     * 判断分类名称是否已存在（排除指定 ID）。
     *
     * @param name      分类名称
     * @param excludeId 需要排除的分类 ID，为 null 时不排除
     * @return 是否存在同名分类
     */
    boolean existsByName(String name, CategoryId excludeId);

    /**
     * 保存分类（新增或更新）。
     *
     * @param category 分类聚合根
     * @return 保存后的分类
     */
    Category save(Category category);

    /**
     * 生成下一个分类 ID。
     *
     * @return 分类 ID
     */
    Long nextId();
}
