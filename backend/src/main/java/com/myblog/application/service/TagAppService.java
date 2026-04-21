package com.myblog.application.service;

import com.myblog.application.dto.TagDTO;
import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;
import com.myblog.domain.repository.TagRepository;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagAppService {

    private final TagRepository tagRepository;

    public TagAppService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagDTO> getTags(Boolean enabled) {
        List<Tag> tags = tagRepository.findAll(enabled);
        return tags.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
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
        return toDTO(tag);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(new TagId(id))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "标签不存在"));
        tag.delete();
        tagRepository.save(tag);
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