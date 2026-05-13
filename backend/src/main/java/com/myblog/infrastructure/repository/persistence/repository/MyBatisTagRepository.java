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

/**
 * 标签 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisTagRepository implements TagRepository {

    private final TagMapper tagMapper;

    /**
     * 创建标签 MyBatis 仓储。
     *
     * @param tagMapper 标签 Mapper
     */
    public MyBatisTagRepository(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    /**
     * 根据标签 ID 查询标签。
     *
     * @param tagId 标签 ID
     * @return 标签 Optional
     */
    @Override
    public Optional<Tag> findById(TagId tagId) {
        TagDO tagDO = tagMapper.selectById(tagId.getValue());
        if (tagDO == null) {
            return Optional.empty();
        }
        return Optional.of(TagPersistenceConverter.toDomain(tagDO));
    }

    /**
     * 查询所有标签。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @return 标签列表
     */
    @Override
    public List<Tag> findAll(Boolean enabled) {
        List<TagDO> tagDOList = tagMapper.selectAll(enabled);
        List<Tag> tags = new ArrayList<>(tagDOList.size());
        for (TagDO tagDO : tagDOList) {
            tags.add(TagPersistenceConverter.toDomain(tagDO));
        }
        return tags;
    }

    /**
     * 分页查询标签列表。
     *
     * @param enabled  是否启用，null 表示不过滤
     * @param keyword  关键字
     * @param page     页码
     * @param pageSize 每页大小
     * @return 标签列表
     */
    @Override
    public List<Tag> findPage(Boolean enabled, String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<TagDO> tagDOList = tagMapper.selectPage(enabled, keyword, offset, currentPageSize);
        List<Tag> tags = new ArrayList<>(tagDOList.size());
        for (TagDO tagDO : tagDOList) {
            tags.add(TagPersistenceConverter.toDomain(tagDO));
        }
        return tags;
    }

    /**
     * 统计标签总数。
     *
     * @param enabled 是否启用，null 表示不过滤
     * @param keyword 关键字
     * @return 标签总数
     */
    @Override
    public long count(Boolean enabled, String keyword) {
        return tagMapper.countAll(enabled, keyword);
    }

    /**
     * 判断标签名称是否存在（可排除指定 ID）。
     *
     * @param name      标签名称
     * @param excludeId 排除的标签 ID
     * @return 是否存在
     */
    @Override
    public boolean existsByName(String name, TagId excludeId) {
        Long excludeIdValue = excludeId != null ? excludeId.getValue() : null;
        return tagMapper.countByName(name, excludeIdValue) > 0;
    }

    /**
     * 保存标签。
     *
     * @param tag 标签聚合根
     * @return 保存后的标签
     */
    @Override
    public Tag save(Tag tag) {
        TagDO tagDO = TagPersistenceConverter.toData(tag);
        tagMapper.insertOrUpdate(tagDO);
        return tag;
    }

    /**
     * 生成下一个标签 ID。
     *
     * @return 标签 ID
     */
    @Override
    public Long nextId() {
        return tagMapper.selectNextId();
    }

    /**
     * 查询热门标签列表。
     *
     * @param limit 返回数量限制
     * @return 热门标签列表
     */
    @Override
    public List<Tag> findHot(int limit) {
        List<TagDO> tagDOList = tagMapper.selectHot(limit);
        List<Tag> tags = new ArrayList<>(tagDOList.size());
        for (TagDO tagDO : tagDOList) {
            tags.add(TagPersistenceConverter.toDomain(tagDO));
        }
        return tags;
    }
}
