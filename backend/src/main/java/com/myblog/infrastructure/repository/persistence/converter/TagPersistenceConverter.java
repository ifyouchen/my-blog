package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;
import com.myblog.infrastructure.repository.persistence.entity.TagDO;

public class TagPersistenceConverter {

    public static Tag toDomain(TagDO tagDO) {
        if (tagDO == null) {
            return null;
        }
        return Tag.restore(
            tagDO.getId(),
            tagDO.getName(),
            tagDO.getDescription(),
            tagDO.getEnabled(),
            tagDO.getCreatedAt(),
            tagDO.getUpdatedAt(),
            tagDO.getDeletedAt(),
            tagDO.getVersion()
        );
    }

    public static TagDO toData(Tag tag) {
        if (tag == null) {
            return null;
        }
        TagDO tagDO = new TagDO();
        tagDO.setId(tag.getId().getValue());
        tagDO.setName(tag.getName());
        tagDO.setDescription(tag.getDescription());
        tagDO.setEnabled(tag.getEnabled());
        tagDO.setCreatedAt(tag.getCreatedAt());
        tagDO.setUpdatedAt(tag.getUpdatedAt());
        tagDO.setDeletedAt(tag.getDeletedAt());
        tagDO.setVersion(tag.getVersion());
        return tagDO;
    }
}