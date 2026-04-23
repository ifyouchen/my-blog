package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.ColumnSubscriptionId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 专栏订阅聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ColumnSubscription {

    private ColumnSubscriptionId id;
    private ColumnId columnId;
    private UserId userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private ColumnSubscription() {
    }

    public static ColumnSubscription create(Long id, ColumnId columnId, UserId userId) {
        ColumnSubscription subscription = new ColumnSubscription();
        subscription.id = new ColumnSubscriptionId(id);
        subscription.columnId = columnId;
        subscription.userId = userId;
        subscription.createdAt = LocalDateTime.now();
        subscription.updatedAt = subscription.createdAt;
        subscription.deletedAt = null;
        subscription.version = 0;
        return subscription;
    }

    public static ColumnSubscription restore(Long id, ColumnId columnId, UserId userId,
                                             LocalDateTime createdAt, LocalDateTime updatedAt,
                                             LocalDateTime deletedAt, Integer version) {
        ColumnSubscription subscription = new ColumnSubscription();
        subscription.id = new ColumnSubscriptionId(id);
        subscription.columnId = columnId;
        subscription.userId = userId;
        subscription.createdAt = createdAt;
        subscription.updatedAt = updatedAt;
        subscription.deletedAt = deletedAt;
        subscription.version = version;
        return subscription;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public ColumnSubscriptionId getId() {
        return id;
    }

    public ColumnId getColumnId() {
        return columnId;
    }

    public UserId getUserId() {
        return userId;
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
