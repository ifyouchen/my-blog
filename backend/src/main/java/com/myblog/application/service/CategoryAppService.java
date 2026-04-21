package com.myblog.application.service;

import com.myblog.application.dto.CategoryDTO;
import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;
import com.myblog.domain.repository.CategoryRepository;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryAppService {

    private final CategoryRepository categoryRepository;

    public CategoryAppService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDTO> getCategories(Boolean enabled) {
        List<Category> categories = categoryRepository.findAll(enabled);
        return categories.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public CategoryDTO getCategory(Long id) {
        Category category = categoryRepository.findById(new CategoryId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类不存在"));
        return toDTO(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryDTO createCategory(String name, String description, Integer sortOrder) {
        if (categoryRepository.existsByName(name, null)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "分类名称已存在");
        }

        Category category = Category.create(
            categoryRepository.nextId(),
            name,
            description,
            sortOrder != null ? sortOrder : 0
        );
        categoryRepository.save(category);
        return toDTO(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryDTO updateCategory(Long id, String name, String description, Integer sortOrder, Boolean enabled) {
        Category category = categoryRepository.findById(new CategoryId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类不存在"));

        if (categoryRepository.existsByName(name, category.getId())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "分类名称已存在");
        }

        category.update(name, description, sortOrder, enabled);
        categoryRepository.save(category);
        return toDTO(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(new CategoryId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类不存在"));
        category.delete();
        categoryRepository.save(category);
    }

    private CategoryDTO toDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId().getValue());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSortOrder(category.getSortOrder());
        dto.setEnabled(category.getEnabled());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }
}