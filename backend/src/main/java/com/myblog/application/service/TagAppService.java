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

@Service
public class TagAppService {

    private final TagRepository tagRepository;
    private final Cache<String, List<TagDTO>> tagsCache;

    public TagAppService(TagRepository tagRepository,
                         @Qualifier("tagsCache") Cache<String, List<TagDTO>> tagsCache) {
        this.tagRepository = tagRepository;
        this.tagsCache = tagsCache;
    }

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
     * @return 标签分页结果
     */
    public PageResult<TagDTO> getTagPage(int page, int pageSize, Boolean enabled) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        long total = tagRepository.count(enabled);
        List<TagDTO> items = tagRepository.findPage(enabled, currentPage, currentPageSize).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return new PageResult<TagDTO>(items, currentPage, currentPageSize, total);
    }

    public TagDTO getTag(Long id) {
        Tag tag = tagRepository.findById(new TagId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "标签不存在"));
        return toDTO(tag);
    }

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

    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(new TagId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "标签不存在"));
        tag.delete();
        tagRepository.save(tag);
        invalidateTagCache();
    }

    private String buildCacheKey(Boolean enabled) {
        return enabled == null ? "enabled:all" : "enabled:" + enabled;
    }

    private void invalidateTagCache() {
        tagsCache.invalidateAll();
    }

    private List<TagDTO> copyTags(List<TagDTO> source) {
        List<TagDTO> copies = new ArrayList<TagDTO>(source.size());
        for (TagDTO item : source) {
            copies.add(copyTag(item));
        }
        return copies;
    }

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
