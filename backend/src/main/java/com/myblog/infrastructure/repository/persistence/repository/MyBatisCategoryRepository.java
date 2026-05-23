package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;
import com.myblog.domain.repository.CategoryRepository;
import com.myblog.infrastructure.repository.persistence.converter.CategoryPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.CategoryDO;
import com.myblog.infrastructure.repository.persistence.mapper.CategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分类 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisCategoryRepository implements CategoryRepository {

    private final CategoryMapper categoryMapper;
    private final IdGenerator idGenerator;

    /**
     * 创建分类 MyBatis 仓储。
     *
     * @param categoryMapper 分类 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisCategoryRepository(CategoryMapper categoryMapper, IdGenerator idGenerator) {
        this.categoryMapper = categoryMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据分类 ID 查询分类。
     *
     * @param categoryId 分类 ID
     * @return 分类 Optional
     */
    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        CategoryDO categoryDO = categoryMapper.selectById(categoryId.getValue());
        if (categoryDO == null) {
            return Optional.empty();
        }
        return Optional.of(CategoryPersistenceConverter.toDomain(categoryDO));
    }

    /**
     * 查询所有分类。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @return 分类列表
     */
    @Override
    public List<Category> findAll(Boolean enabled) {
        List<CategoryDO> categoryDOList = categoryMapper.selectAll(enabled);
        List<Category> categories = new ArrayList<>(categoryDOList.size());
        for (CategoryDO categoryDO : categoryDOList) {
            categories.add(CategoryPersistenceConverter.toDomain(categoryDO));
        }
        return categories;
    }

    /**
     * 分页查询分类列表。
     *
     * @param enabled  是否启用，null 表示不过滤
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分类列表
     */
    @Override
    public List<Category> findPage(Boolean enabled, Long groupId, String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<CategoryDO> categoryDOList = categoryMapper.selectPage(enabled, groupId, keyword, offset,
            currentPageSize);
        List<Category> categories = new ArrayList<>(categoryDOList.size());
        for (CategoryDO categoryDO : categoryDOList) {
            categories.add(CategoryPersistenceConverter.toDomain(categoryDO));
        }
        return categories;
    }

    /**
     * 统计分类总数。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @return 分类总数
     */
    @Override
    public long count(Boolean enabled, Long groupId, String keyword) {
        return categoryMapper.countAll(enabled, groupId, keyword);
    }

    /**
     * 判断分类名称是否存在（可排除指定 ID）。
     *
     * @param name      分类名称
     * @param excludeId 排除的分类 ID
     * @return 是否存在
     */
    @Override
    public boolean existsByName(String name, CategoryId excludeId) {
        Long excludeIdValue = excludeId != null ? excludeId.getValue() : null;
        return categoryMapper.countByName(name, excludeIdValue) > 0;
    }

    /**
     * 判断启用分类名称是否存在，且所属分类组也启用。
     *
     * @param name 分类名称
     * @return 是否存在
     */
    @Override
    public boolean existsEnabledByName(String name) {
        return categoryMapper.countEnabledByName(name) > 0;
    }

    /**
     * 统计指定分类组下的小分类数量。
     *
     * @param groupId 分类组 ID
     * @return 小分类数量
     */
    @Override
    public long countByGroupId(Long groupId) {
        return categoryMapper.countByGroupId(groupId);
    }

    /**
     * 保存分类。
     *
     * @param category 分类聚合根
     * @return 保存后的分类
     */
    @Override
    public Category save(Category category) {
        CategoryDO categoryDO = CategoryPersistenceConverter.toData(category);
        categoryMapper.insertOrUpdate(categoryDO);
        return category;
    }

    /**
     * 生成下一个分类 ID。
     *
     * @return 分类 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_category");
    }

    @Override
    public List<Map<String, Object>> findDistinctGroups() {
        return categoryMapper.selectDistinctGroups();
    }

    @Override
    public void renameGroup(String oldName, String newName) {
        categoryMapper.renameGroup(oldName, newName);
    }

    @Override
    public void clearGroup(String groupName) {
        categoryMapper.clearGroup(groupName);
    }
}
