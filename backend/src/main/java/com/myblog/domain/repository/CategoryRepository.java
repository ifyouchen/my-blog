package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(CategoryId categoryId);

    List<Category> findAll(Boolean enabled);

    boolean existsByName(String name, CategoryId excludeId);

    Category save(Category category);

    Long nextId();
}