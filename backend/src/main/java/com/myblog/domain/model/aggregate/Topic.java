package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.TopicId;

import java.time.LocalDateTime;

/**
 * 专题聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Topic {

    private TopicId id;
    private String title;
    private String summary;
    private String coverUrl;
    private String status;
    private Integer sortOrder;
    private Integer articleCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private Topic() {
    }

    public static Topic create(Long id, String title, String summary,
                               String coverUrl, Integer sortOrder) {
        Topic topic = new Topic();
        topic.id = new TopicId(id);
        topic.title = title;
        topic.summary = summary;
        topic.coverUrl = coverUrl;
        topic.status = "PUBLISHED";
        topic.sortOrder = sortOrder == null ? 0 : sortOrder;
        topic.articleCount = 0;
        topic.createdAt = LocalDateTime.now();
        topic.updatedAt = topic.createdAt;
        topic.deletedAt = null;
        topic.version = 0;
        return topic;
    }

    public static Topic restore(Long id, String title, String summary,
                                String coverUrl, String status, Integer sortOrder,
                                Integer articleCount,
                                LocalDateTime createdAt, LocalDateTime updatedAt,
                                LocalDateTime deletedAt, Integer version) {
        Topic topic = new Topic();
        topic.id = new TopicId(id);
        topic.title = title;
        topic.summary = summary;
        topic.coverUrl = coverUrl;
        topic.status = status;
        topic.sortOrder = sortOrder == null ? 0 : sortOrder;
        topic.articleCount = articleCount == null ? 0 : articleCount;
        topic.createdAt = createdAt;
        topic.updatedAt = updatedAt;
        topic.deletedAt = deletedAt;
        topic.version = version;
        return topic;
    }

    public void update(String title, String summary, String coverUrl,
                       Integer sortOrder, String status) {
        this.title = title;
        this.summary = summary;
        this.coverUrl = coverUrl;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    public boolean isPublished() {
        return "PUBLISHED".equals(status) && deletedAt == null;
    }

    public TopicId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public Integer getArticleCount() {
        return articleCount == null ? 0 : articleCount;
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
