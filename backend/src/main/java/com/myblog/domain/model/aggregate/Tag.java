package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.TagId;

import java.time.LocalDateTime;

public class Tag {

    private TagId id;
    private String name;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private Tag() {
    }

    public static Tag create(Long id, String name, String description) {
        Tag tag = new Tag();
        tag.id = new TagId(id);
        tag.name = name;
        tag.description = description;
        tag.enabled = true;
        tag.createdAt = LocalDateTime.now();
        tag.updatedAt = tag.createdAt;
        tag.deletedAt = null;
        tag.version = 0;
        return tag;
    }

    public static Tag restore(Long id, String name, String description, Boolean enabled,
                             LocalDateTime createdAt, LocalDateTime updatedAt,
                             LocalDateTime deletedAt, Integer version) {
        Tag tag = new Tag();
        tag.id = new TagId(id);
        tag.name = name;
        tag.description = description;
        tag.enabled = enabled;
        tag.createdAt = createdAt;
        tag.updatedAt = updatedAt;
        tag.deletedAt = deletedAt;
        tag.version = version;
        return tag;
    }

    public void update(String name, String description, Boolean enabled) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = this.version + 1;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public TagId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Integer getVersion() {
        return version;
    }
}