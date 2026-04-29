package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 专栏聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Column {

    private ColumnId id;
    private UserId authorId;
    private String title;
    private String summary;
    private String coverUrl;
    private String status;
    private Integer sortOrder;
    private Integer subscriberCount;
    private Integer articleCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private Column() {
    }

    public static Column create(Long id, UserId authorId, String title, String summary,
                                String coverUrl, Integer sortOrder) {
        Column column = new Column();
        column.id = new ColumnId(id);
        column.authorId = authorId;
        column.title = title;
        column.summary = summary;
        column.coverUrl = coverUrl;
        column.status = "PUBLISHED";
        column.sortOrder = sortOrder == null ? 0 : sortOrder;
        column.subscriberCount = 0;
        column.articleCount = 0;
        column.createdAt = LocalDateTime.now();
        column.updatedAt = column.createdAt;
        column.deletedAt = null;
        column.version = 0;
        return column;
    }

    public static Column restore(Long id, UserId authorId, String title, String summary,
                                 String coverUrl, String status, Integer sortOrder,
                                 Integer subscriberCount, Integer articleCount,
                                 LocalDateTime createdAt, LocalDateTime updatedAt,
                                 LocalDateTime deletedAt, Integer version) {
        Column column = new Column();
        column.id = new ColumnId(id);
        column.authorId = authorId;
        column.title = title;
        column.summary = summary;
        column.coverUrl = coverUrl;
        column.status = status;
        column.sortOrder = sortOrder == null ? 0 : sortOrder;
        column.subscriberCount = subscriberCount == null ? 0 : subscriberCount;
        column.articleCount = articleCount == null ? 0 : articleCount;
        column.createdAt = createdAt;
        column.updatedAt = updatedAt;
        column.deletedAt = deletedAt;
        column.version = version;
        return column;
    }

    /**
     * 更新专栏基本信息。
     */
    public void update(String title, String summary, String coverUrl, Integer sortOrder, String status) {
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

    /**
     * 软删除专栏。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    public void increaseSubscriberCount() {
        this.subscriberCount = getSubscriberCount() + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseSubscriberCount() {
        this.subscriberCount = Math.max(0, getSubscriberCount() - 1);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPublished() {
        return "PUBLISHED".equals(status) && deletedAt == null;
    }

    public ColumnId getId() {
        return id;
    }

    public UserId getAuthorId() {
        return authorId;
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

    public Integer getSubscriberCount() {
        return subscriberCount == null ? 0 : subscriberCount;
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
