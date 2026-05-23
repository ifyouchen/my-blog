package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;
import com.myblog.infrastructure.repository.persistence.entity.TagDO;

/**
 * 标签持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public class TagPersistenceConverter {

    /**
     * 将标签数据对象转换为领域对象。
     *
     * @param tagDO 标签数据对象
     * @return 标签领域对象，若 tagDO 为 null 则返回 null
     */
    public static Tag toDomain(TagDO tagDO) {
        if (tagDO == null) {
            return null;
        }
        return Tag.restore(
            tagDO.getId(),
            tagDO.getName(),
            tagDO.getDescription(),
            tagDO.getGroupName(),
            tagDO.getEnabled(),
            tagDO.getCreatedAt(),
            tagDO.getUpdatedAt(),
            tagDO.getDeletedAt(),
            tagDO.getVersion()
        );
    }

    /**
     * 将标签领域对象转换为数据对象。
     *
     * @param tag 标签领域对象
     * @return 标签数据对象，若 tag 为 null 则返回 null
     */
    public static TagDO toData(Tag tag) {
        if (tag == null) {
            return null;
        }
        TagDO tagDO = new TagDO();
        tagDO.setId(tag.getId().getValue());
        tagDO.setName(tag.getName());
        tagDO.setDescription(tag.getDescription());
        tagDO.setGroupName(tag.getGroupName());
        tagDO.setEnabled(tag.getEnabled());
        tagDO.setCreatedAt(tag.getCreatedAt());
        tagDO.setUpdatedAt(tag.getUpdatedAt());
        tagDO.setDeletedAt(tag.getDeletedAt());
        tagDO.setVersion(tag.getVersion());
        return tagDO;
    }
}