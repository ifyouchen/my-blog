package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.CategoryDTO;
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

/**
 * 分类应用服务。
 * <p>
 * 提供分类的查询、创建、更新、删除功能。
 * 内置 Caffeine 缓存对全量分类列表进行缓存，每次分类发生变更时会同步失效首页启动缓存。
 * </p>
 */
@Service
public class CategoryAppService {

    private final CategoryRepository categoryRepository;
    private final Cache<String, List<CategoryDTO>> categoriesCache;
    private final HomePortalCacheInvalidator homePortalCacheInvalidator;

    public CategoryAppService(CategoryRepository categoryRepository,
                              @Qualifier("categoriesCache") Cache<String, List<CategoryDTO>> categoriesCache,
                              HomePortalCacheInvalidator homePortalCacheInvalidator) {
        this.categoryRepository = categoryRepository;
        this.categoriesCache = categoriesCache;
        this.homePortalCacheInvalidator = homePortalCacheInvalidator;
    }

    /**
     * 查询分类列表，优先从缓存返回。
     *
     * @param enabled 启用状态筛选，null 表示查询所有
     * @return 分类 DTO 列表
     */
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

    /**
     * 根据 ID 查询分类详情。
     *
     * @param id 分类 ID
     * @return 分类 DTO
     * @throws ApplicationException 分类不存在时抛出
     */
    public CategoryDTO getCategory(Long id) {
        Category category = categoryRepository.findById(new CategoryId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类不存在"));
        return toDTO(category);
    }

    /**
     * 创建分类。
     *
     * @param name        分类名称，不允许重复
     * @param description 分类描述，可为 null
     * @param sortOrder   排序顺序，null 时默认为 0
     * @return 创建后的分类 DTO
     * @throws ApplicationException 分类名称已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryDTO createCategory(String name, String groupName, String description, Integer sortOrder) {
        if (categoryRepository.existsByName(name, null)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "分类名称已存在");
        }

        Category category = Category.create(
            categoryRepository.nextId(),
            name,
            groupName,
            description,
            sortOrder != null ? sortOrder : 0
        );
        categoryRepository.save(category);
        invalidateCategoryCache();
        return toDTO(category);
    }

    /**
     * 更新分类信息。
     *
     * @param id          分类 ID
     * @param name        新的分类名称
     * @param description 新的分类描述
     * @param sortOrder   新的排序顺序
     * @param enabled     是否启用
     * @return 更新后的分类 DTO
     * @throws ApplicationException 分类不存在或名称重复时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryDTO updateCategory(Long id, String name, String groupName, String description, Integer sortOrder, Boolean enabled) {
        Category category = categoryRepository.findById(new CategoryId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类不存在"));

        if (categoryRepository.existsByName(name, category.getId())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "分类名称已存在");
        }

        category.update(name, groupName, description, sortOrder, enabled);
        categoryRepository.save(category);
        invalidateCategoryCache();
        return toDTO(category);
    }

    /**
     * 删除指定分类并失效缓存。
     *
     * @param id 分类 ID
     * @throws ApplicationException 分类不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(new CategoryId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类不存在"));
        category.delete();
        categoryRepository.save(category);
        invalidateCategoryCache();
    }

    /**
     * 根据启用状态构建缓存键。
     *
     * @param enabled 启用状态，null 表示查询全部
     * @return 缓存键字符串
     */
    private String buildCacheKey(Boolean enabled) {
        return enabled == null ? "enabled:all" : "enabled:" + enabled;
    }

    /**
     * 失效分类缓存并同步失效首页启动缓存。
     */
    private void invalidateCategoryCache() {
        categoriesCache.invalidateAll();
        homePortalCacheInvalidator.evictBootstrap();
    }

    /**
     * 深拷贝分类 DTO 列表，防止缓存对象被外部修改。
     *
     * @param source 原始列表
     * @return 拷贝后的新列表
     */
    private List<CategoryDTO> copyCategories(List<CategoryDTO> source) {
        List<CategoryDTO> copies = new ArrayList<CategoryDTO>(source.size());
        for (CategoryDTO item : source) {
            copies.add(copyCategory(item));
        }
        return copies;
    }

    /**
     * 深拷贝单个分类 DTO。
     *
     * @param source 原始 DTO
     * @return 拷贝后的新 DTO
     */
    private CategoryDTO copyCategory(CategoryDTO source) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setGroupName(source.getGroupName());
        dto.setDescription(source.getDescription());
        dto.setSortOrder(source.getSortOrder());
        dto.setEnabled(source.getEnabled());
        dto.setCreatedAt(source.getCreatedAt());
        dto.setUpdatedAt(source.getUpdatedAt());
        return dto;
    }

    /**
     * 将分类领域对象转换为 DTO。
     *
     * @param category 分类领域对象
     * @return 分类 DTO
     */
    private CategoryDTO toDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId().getValue());
        dto.setName(category.getName());
        dto.setGroupName(category.getGroupName());
        dto.setDescription(category.getDescription());
        dto.setSortOrder(category.getSortOrder());
        dto.setEnabled(category.getEnabled());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }
}
