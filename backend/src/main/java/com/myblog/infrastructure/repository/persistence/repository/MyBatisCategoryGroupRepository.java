package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.CategoryGroup;
import com.myblog.domain.model.valueobject.CategoryGroupId;
import com.myblog.domain.repository.CategoryGroupRepository;
import com.myblog.infrastructure.repository.persistence.converter.CategoryGroupPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.CategoryGroupDO;
import com.myblog.infrastructure.repository.persistence.mapper.CategoryGroupMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 分类组 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisCategoryGroupRepository implements CategoryGroupRepository {

    private final CategoryGroupMapper categoryGroupMapper;
    private final IdGenerator idGenerator;

    /**
     * 创建分类组 MyBatis 仓储。
     *
     * @param categoryGroupMapper 分类组 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisCategoryGroupRepository(CategoryGroupMapper categoryGroupMapper, IdGenerator idGenerator) {
        this.categoryGroupMapper = categoryGroupMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据 ID 查询分类组。
     *
     * @param id 分类组 ID
     * @return 分类组 Optional
     */
    @Override
    public Optional<CategoryGroup> findById(CategoryGroupId id) {
        CategoryGroupDO groupDO = categoryGroupMapper.selectById(id.getValue());
        if (groupDO == null) {
            return Optional.empty();
        }
        return Optional.of(CategoryGroupPersistenceConverter.toDomain(groupDO));
    }

    /**
     * 根据名称查询分类组。
     *
     * @param name 分类组名称
     * @return 分类组 Optional
     */
    @Override
    public Optional<CategoryGroup> findByName(String name) {
        CategoryGroupDO groupDO = categoryGroupMapper.selectByName(name);
        if (groupDO == null) {
            return Optional.empty();
        }
        return Optional.of(CategoryGroupPersistenceConverter.toDomain(groupDO));
    }

    /**
     * 查询分类组列表。
     *
     * @param enabled 是否启用，null 表示全部
     * @return 分类组列表
     */
    @Override
    public List<CategoryGroup> findAll(Boolean enabled) {
        List<CategoryGroupDO> groupDOList = categoryGroupMapper.selectAll(enabled);
        List<CategoryGroup> groups = new ArrayList<CategoryGroup>(groupDOList.size());
        for (CategoryGroupDO groupDO : groupDOList) {
            groups.add(CategoryGroupPersistenceConverter.toDomain(groupDO));
        }
        return groups;
    }

    /**
     * 判断分类组名称是否存在。
     *
     * @param name 分类组名称
     * @param excludeId 排除 ID
     * @return 是否存在
     */
    @Override
    public boolean existsByName(String name, CategoryGroupId excludeId) {
        Long excludeIdValue = excludeId == null ? null : excludeId.getValue();
        return categoryGroupMapper.countByName(name, excludeIdValue) > 0;
    }

    /**
     * 保存分类组。
     *
     * @param group 分类组
     * @return 分类组
     */
    @Override
    public CategoryGroup save(CategoryGroup group) {
        categoryGroupMapper.insertOrUpdate(CategoryGroupPersistenceConverter.toData(group));
        return group;
    }

    /**
     * 生成下一个分类组 ID。
     *
     * @return 分类组 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_category_group");
    }
}
