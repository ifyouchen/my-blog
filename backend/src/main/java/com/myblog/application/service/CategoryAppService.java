package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.valueobject.CategoryId;
import com.myblog.domain.repository.CategoryRepository;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryAppService {

    private final CategoryRepository categoryRepository;
    private final Cache<String, List<CategoryDTO>> categoriesCache;
    private final Cache<String, HomeBootstrapDTO> homeBootstrapCache;

    public CategoryAppService(CategoryRepository categoryRepository,
                              @Qualifier("categoriesCache") Cache<String, List<CategoryDTO>> categoriesCache,
                              @Qualifier("homeBootstrapCache") Cache<String, HomeBootstrapDTO> homeBootstrapCache) {
        this.categoryRepository = categoryRepository;
        this.categoriesCache = categoriesCache;
        this.homeBootstrapCache = homeBootstrapCache;
    }

    public List<CategoryDTO> getCategories(Boolean enabled) {
        String cacheKey = buildCacheKey(enabled);
        List<CategoryDTO> cached = categoriesCache.getIfPresent(cacheKey);
        if (cached != null) {
            return copyCategories(cached);
        }
        List<CategoryDTO> items = categoryRepository.findAll(enabled).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        categoriesCache.put(cacheKey, copyCategories(items));
        return items;
    }

    /**
     * 分页查询分类列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param enabled 启用状态
     * @return 分类分页结果
     */
    public PageResult<CategoryDTO> getCategoryPage(int page, int pageSize, Boolean enabled) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        long total = categoryRepository.count(enabled);
        List<CategoryDTO> items = categoryRepository.findPage(enabled, currentPage, currentPageSize).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return new PageResult<CategoryDTO>(items, currentPage, currentPageSize, total);
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
        invalidateCategoryCache();
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
        invalidateCategoryCache();
        return toDTO(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(new CategoryId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类不存在"));
        category.delete();
        categoryRepository.save(category);
        invalidateCategoryCache();
    }

    private String buildCacheKey(Boolean enabled) {
        return enabled == null ? "enabled:all" : "enabled:" + enabled;
    }

    private void invalidateCategoryCache() {
        categoriesCache.invalidateAll();
        homeBootstrapCache.invalidateAll();
    }

    private List<CategoryDTO> copyCategories(List<CategoryDTO> source) {
        List<CategoryDTO> copies = new ArrayList<CategoryDTO>(source.size());
        for (CategoryDTO item : source) {
            copies.add(copyCategory(item));
        }
        return copies;
    }

    private CategoryDTO copyCategory(CategoryDTO source) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setDescription(source.getDescription());
        dto.setSortOrder(source.getSortOrder());
        dto.setEnabled(source.getEnabled());
        dto.setCreatedAt(source.getCreatedAt());
        dto.setUpdatedAt(source.getUpdatedAt());
        return dto;
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
