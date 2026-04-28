package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;
import com.myblog.domain.repository.TagRepository;
import com.myblog.infrastructure.repository.persistence.converter.TagPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.TagDO;
import com.myblog.infrastructure.repository.persistence.mapper.TagMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Repository
@Profile("!memory")
public class MyBatisTagRepository implements TagRepository {

    private final TagMapper tagMapper;

    public MyBatisTagRepository(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public Optional<Tag> findById(TagId tagId) {
        TagDO tagDO = tagMapper.selectById(tagId.getValue());
        if (tagDO == null) {
            return Optional.empty();
        }
        return Optional.of(TagPersistenceConverter.toDomain(tagDO));
    }

    @Override
    public List<Tag> findAll(Boolean enabled) {
        List<TagDO> tagDOList = tagMapper.selectAll(enabled);
        List<Tag> tags = new ArrayList<>(tagDOList.size());
        for (TagDO tagDO : tagDOList) {
            tags.add(TagPersistenceConverter.toDomain(tagDO));
        }
        return tags;
    }

    @Override
    public List<Tag> findPage(Boolean enabled, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<TagDO> tagDOList = tagMapper.selectPage(enabled, offset, currentPageSize);
        List<Tag> tags = new ArrayList<>(tagDOList.size());
        for (TagDO tagDO : tagDOList) {
            tags.add(TagPersistenceConverter.toDomain(tagDO));
        }
        return tags;
    }

    @Override
    public long count(Boolean enabled) {
        return tagMapper.countAll(enabled);
    }

    @Override
    public boolean existsByName(String name, TagId excludeId) {
        Long excludeIdValue = excludeId != null ? excludeId.getValue() : null;
        return tagMapper.countByName(name, excludeIdValue) > 0;
    }

    @Override
    public Tag save(Tag tag) {
        TagDO tagDO = TagPersistenceConverter.toData(tag);
        tagMapper.insertOrUpdate(tagDO);
        return tag;
    }

    @Override
    public Long nextId() {
        return tagMapper.selectNextId();
    }
}
