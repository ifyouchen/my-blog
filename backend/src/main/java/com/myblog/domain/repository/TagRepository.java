package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.valueobject.TagId;

import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Optional<Tag> findById(TagId tagId);

    List<Tag> findAll(Boolean enabled);

    boolean existsByName(String name, TagId excludeId);

    Tag save(Tag tag);

    Long nextId();
}