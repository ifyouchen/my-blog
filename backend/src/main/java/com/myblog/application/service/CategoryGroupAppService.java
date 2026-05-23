package com.myblog.application.service;

import com.myblog.application.dto.CategoryGroupDTO;
import com.myblog.domain.model.aggregate.CategoryGroup;
import com.myblog.domain.model.valueobject.CategoryGroupId;
import com.myblog.domain.repository.CategoryGroupRepository;
import com.myblog.domain.repository.CategoryRepository;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类组应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class CategoryGroupAppService {

    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryRepository categoryRepository;
    private final HomePortalCacheInvalidator homePortalCacheInvalidator;

    /**
     * 创建分类组应用服务。
     *
     * @param categoryGroupRepository 分类组仓储
     * @param categoryRepository 分类仓储
     * @param homePortalCacheInvalidator 首页缓存失效器
     */
    public CategoryGroupAppService(CategoryGroupRepository categoryGroupRepository,
                                   CategoryRepository categoryRepository,
                                   HomePortalCacheInvalidator homePortalCacheInvalidator) {
        this.categoryGroupRepository = categoryGroupRepository;
        this.categoryRepository = categoryRepository;
        this.homePortalCacheInvalidator = homePortalCacheInvalidator;
    }

    /**
     * 查询分类组列表。
     *
     * @param enabled 是否启用，null 表示全部
     * @return 分类组列表
     */
    @Transactional(readOnly = true)
    public List<CategoryGroupDTO> listGroups(Boolean enabled) {
        List<CategoryGroup> groups = categoryGroupRepository.findAll(enabled);
        List<CategoryGroupDTO> result = new ArrayList<CategoryGroupDTO>(groups.size());
        for (CategoryGroup group : groups) {
            CategoryGroupDTO dto = toDTO(group);
            dto.setCategoryCount(categoryRepository.countByGroupId(group.getId().getValue()));
            result.add(dto);
        }
        return result;
    }

    /**
     * 查询分类组详情。
     *
     * @param id 分类组 ID
     * @return 分类组 DTO
     */
    @Transactional(readOnly = true)
    public CategoryGroupDTO getGroup(Long id) {
        CategoryGroup group = categoryGroupRepository.findById(new CategoryGroupId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类组不存在"));
        CategoryGroupDTO dto = toDTO(group);
        dto.setCategoryCount(categoryRepository.countByGroupId(id));
        return dto;
    }

    /**
     * 创建分类组。
     *
     * @param name 分类组名称
     * @param description 分类组说明
     * @param sortOrder 排序值
     * @return 分类组 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryGroupDTO createGroup(String name, String description, Integer sortOrder) {
        String normalizedName = normalizeRequiredText(name, "分类组名称不能为空");
        String normalizedDescription = normalizeNullableText(description);
        if (categoryGroupRepository.existsByName(normalizedName, null)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "分类组名称已存在");
        }
        CategoryGroup group = CategoryGroup.create(
            categoryGroupRepository.nextId(),
            normalizedName,
            normalizedDescription,
            sortOrder == null ? 0 : sortOrder
        );
        categoryGroupRepository.save(group);
        homePortalCacheInvalidator.evictBootstrap();
        return toDTO(group);
    }

    /**
     * 更新分类组。
     *
     * @param id 分类组 ID
     * @param name 分类组名称
     * @param description 分类组说明
     * @param sortOrder 排序值
     * @param enabled 是否启用
     * @return 分类组 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryGroupDTO updateGroup(Long id, String name, String description, Integer sortOrder,
                                        Boolean enabled) {
        CategoryGroup group = categoryGroupRepository.findById(new CategoryGroupId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类组不存在"));
        String normalizedName = normalizeRequiredText(name, "分类组名称不能为空");
        String normalizedDescription = normalizeNullableText(description);
        if (categoryGroupRepository.existsByName(normalizedName, group.getId())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "分类组名称已存在");
        }
        group.update(normalizedName, normalizedDescription, sortOrder == null ? 0 : sortOrder,
            enabled == null ? Boolean.TRUE : enabled);
        categoryGroupRepository.save(group);
        homePortalCacheInvalidator.evictBootstrap();
        return toDTO(group);
    }

    /**
     * 删除空分类组。
     *
     * @param id 分类组 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        CategoryGroup group = categoryGroupRepository.findById(new CategoryGroupId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类组不存在"));
        if (categoryRepository.countByGroupId(id) > 0) {
            throw new ApplicationException(ErrorCode.CONFLICT, "分类组下仍有小分类，请先迁移或删除小分类");
        }
        group.delete();
        categoryGroupRepository.save(group);
        homePortalCacheInvalidator.evictBootstrap();
    }

    /**
     * 校验分类组存在且启用。
     *
     * @param id 分类组 ID
     * @return 分类组
     */
    @Transactional(readOnly = true)
    public CategoryGroup requireEnabledGroup(Long id) {
        if (id == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请选择分类组");
        }
        CategoryGroup group = categoryGroupRepository.findById(new CategoryGroupId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "分类组不存在"));
        if (!Boolean.TRUE.equals(group.getEnabled())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "分类组已禁用");
        }
        return group;
    }

    private CategoryGroupDTO toDTO(CategoryGroup group) {
        CategoryGroupDTO dto = new CategoryGroupDTO();
        dto.setId(group.getId().getValue());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setSortOrder(group.getSortOrder());
        dto.setEnabled(group.getEnabled());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        return dto;
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
