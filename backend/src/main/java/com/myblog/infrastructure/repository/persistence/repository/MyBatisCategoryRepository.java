package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;
import com.myblog.domain.repository.CategoryRepository;
import com.myblog.infrastructure.repository.persistence.converter.CategoryPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.CategoryDO;
import com.myblog.infrastructure.repository.persistence.mapper.CategoryMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!memory")
public class MyBatisCategoryRepository implements CategoryRepository {

    private final CategoryMapper categoryMapper;

    public MyBatisCategoryRepository(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        CategoryDO categoryDO = categoryMapper.selectById(categoryId.getValue());
        if (categoryDO == null) {
            return Optional.empty();
        }
        return Optional.of(CategoryPersistenceConverter.toDomain(categoryDO));
    }

    @Override
    public List<Category> findAll(Boolean enabled) {
        List<CategoryDO> categoryDOList = categoryMapper.selectAll(enabled);
        List<Category> categories = new ArrayList<>(categoryDOList.size());
        for (CategoryDO categoryDO : categoryDOList) {
            categories.add(CategoryPersistenceConverter.toDomain(categoryDO));
        }
        return categories;
    }

    @Override
    public boolean existsByName(String name, CategoryId excludeId) {
        Long excludeIdValue = excludeId != null ? excludeId.getValue() : null;
        return categoryMapper.countByName(name, excludeIdValue) > 0;
    }

    @Override
    public Category save(Category category) {
        CategoryDO categoryDO = CategoryPersistenceConverter.toData(category);
        if (categoryMapper.countById(category.getId().getValue()) > 0) {
            categoryMapper.update(categoryDO);
        } else {
            categoryMapper.insert(categoryDO);
        }
        return category;
    }

    @Override
    public Long nextId() {
        Long nextId = categoryMapper.selectNextId();
        return nextId == null ? 1L : nextId;
    }
}