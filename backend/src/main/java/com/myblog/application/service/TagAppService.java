package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.TagDTO;
import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;
import com.myblog.domain.repository.TagRepository;
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
 * 标签应用服务。
 * <p>
 * 提供标签的查询、创建、更新、删除功能。
 * 内置 Caffeine 缓存对全量标签列表进行缓存，每次标签发生变更时会自动失效缓存。
 * </p>
 */
@Service
public class TagAppService {

    private final TagRepository tagRepository;
    private final Cache<String, List<TagDTO>> tagsCache;

    public TagAppService(TagRepository tagRepository,
                         @Qualifier("tagsCache") Cache<String, List<TagDTO>> tagsCache) {
        this.tagRepository = tagRepository;
        this.tagsCache = tagsCache;
    }

    /**
     * 查询标签列表，优先从缓存返回。
     *
     * @param enabled 启用状态筛选，null 表示查询所有
     * @return 标签 DTO 列表
     */
    public List<TagDTO> getTags(Boolean enabled) {
        String cacheKey = buildCacheKey(enabled);
        List<TagDTO> cached = tagsCache.getIfPresent(cacheKey);
        if (cached != null) {
            return copyTags(cached);
        }
        List<TagDTO> items = tagRepository.findAll(enabled).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        tagsCache.put(cacheKey, copyTags(items));
        return items;
    }

    /**
     * 分页查询标签列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param enabled 启用状态
     * @param keyword 关键字
     * @return 标签分页结果
     */
    public PageResult<TagDTO> getTagPage(int page, int pageSize, Boolean enabled, String keyword) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        String normalizedKeyword = normalizeKeyword(keyword);
        long total = tagRepository.count(enabled, normalizedKeyword);
        List<TagDTO> items = tagRepository.findPage(enabled, normalizedKeyword, currentPage, currentPageSize).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return new PageResult<TagDTO>(items, currentPage, currentPageSize, total);
    }

    /**
     * 根据 ID 查询标签详情。
     *
     * @param id 标签 ID
     * @return 标签 DTO
     * @throws ApplicationException 标签不存在时抛出
     */
    public TagDTO getTag(Long id) {
        Tag tag = tagRepository.findById(new TagId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "标签不存在"));
        return toDTO(tag);
    }

    /**
     * 创建标签。
     *
     * @param name        标签名称，不允许重复
     * @param description 标签描述，可为 null
     * @return 创建后的标签 DTO
     * @throws ApplicationException 标签名称已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public TagDTO createTag(String name, String description) {
        if (tagRepository.existsByName(name, null)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "标签名称已存在");
        }

        Tag tag = Tag.create(
            tagRepository.nextId(),
            name,
            description
        );
        tagRepository.save(tag);
        invalidateTagCache();
        return toDTO(tag);
    }

    /**
     * 更新标签信息。
     *
     * @param id          标签 ID
     * @param name        新的标签名称
     * @param description 新的标签描述
     * @param enabled     是否启用
     * @return 更新后的标签 DTO
     * @throws ApplicationException 标签不存在或名称重复时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public TagDTO updateTag(Long id, String name, String description, Boolean enabled) {
        Tag tag = tagRepository.findById(new TagId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "标签不存在"));

        if (tagRepository.existsByName(name, tag.getId())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "标签名称已存在");
        }

        tag.update(name, description, enabled);
        tagRepository.save(tag);
        invalidateTagCache();
        return toDTO(tag);
    }

    /**
     * 查询热门标签（按使用次数降序）。
     *
     * @param limit 返回数量，最多 100
     * @return 热门标签列表
     */
    public List<TagDTO> getHotTags(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return tagRepository.findHot(safeLimit).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 删除指定标签并失效缓存。
     *
     * @param id 标签 ID
     * @throws ApplicationException 标签不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(new TagId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "标签不存在"));
        tag.delete();
        tagRepository.save(tag);
        invalidateTagCache();
    }

    /**
     * 根据启用状态构建缓存键。
     *
     * @param enabled 启用状态筛选条件，null 表示查询全部
     * @return 缓存键字符串
     */
    private String buildCacheKey(Boolean enabled) {
        return enabled == null ? "enabled:all" : "enabled:" + enabled;
    }

    /**
     * 失效全部标签缓存。
     */
    private void invalidateTagCache() {
        tagsCache.invalidateAll();
    }

    /**
     * 规范化搜索关键字，去除首尾空白，空字符串转为 null。
     *
     * @param keyword 原始关键字
     * @return 规范化后的关键字，空或 null 时返回 null
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 深拷贝标签 DTO 列表，防止缓存对象被外部修改。
     *
     * @param source 原始列表
     * @return 拷贝后的新列表
     */
    private List<TagDTO> copyTags(List<TagDTO> source) {
        List<TagDTO> copies = new ArrayList<TagDTO>(source.size());
        for (TagDTO item : source) {
            copies.add(copyTag(item));
        }
        return copies;
    }

    /**
     * 深拷贝单个标签 DTO。
     *
     * @param source 原始 DTO
     * @return 拷贝后的新 DTO
     */
    private TagDTO copyTag(TagDTO source) {
        TagDTO dto = new TagDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setDescription(source.getDescription());
        dto.setEnabled(source.getEnabled());
        dto.setCreatedAt(source.getCreatedAt());
        dto.setUpdatedAt(source.getUpdatedAt());
        return dto;
    }

    /**
     * 将标签领域对象转换为 DTO。
     *
     * @param tag 标签领域对象
     * @return 标签 DTO
     */
    private TagDTO toDTO(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId().getValue());
        dto.setName(tag.getName());
        dto.setDescription(tag.getDescription());
        dto.setEnabled(tag.getEnabled());
        dto.setCreatedAt(tag.getCreatedAt());
        dto.setUpdatedAt(tag.getUpdatedAt());
        return dto;
    }
}
