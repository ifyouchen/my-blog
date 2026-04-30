package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;

import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Optional<Tag> findById(TagId tagId);

    List<Tag> findAll(Boolean enabled);

    List<Tag> findPage(Boolean enabled, int page, int pageSize);

    long count(Boolean enabled);

    boolean existsByName(String name, TagId excludeId);

    Tag save(Tag tag);

    Long nextId();

    /**
     * 查询热门标签（按使用次数降序）。
     *
     * @param limit 返回数量
     * @return 热门标签列表
     */
    List<Tag> findHot(int limit);
}
