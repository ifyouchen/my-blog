package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.ColumnSubscriptionId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 专栏订阅聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ColumnSubscription {

    /**
     * 订阅记录 ID
     */
    private ColumnSubscriptionId id;

    /**
     * 被订阅的专栏 ID
     */
    private ColumnId columnId;

    /**
     * 订阅用户 ID
     */
    private UserId userId;

    /**
     * 订阅创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 订阅最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示订阅有效
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private ColumnSubscription() {
    }

    /**
     * 创建专栏订阅聚合根。
     *
     * @param id       订阅记录 ID
     * @param columnId 被订阅的专栏 ID
     * @param userId   订阅用户 ID
     * @return 专栏订阅聚合根
     */
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

    /**
     * 从持久化数据还原专栏订阅聚合根。
     *
     * @param id        订阅记录 ID
     * @param columnId  被订阅的专栏 ID
     * @param userId    订阅用户 ID
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param deletedAt 删除时间
     * @param version   乐观锁版本号
     * @return 专栏订阅聚合根
     */
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

    /**
     * 取消订阅（软删除）。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    /**
     * 重新激活已取消的订阅。
     */
    public void reactivate() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断订阅是否已取消。
     *
     * @return 已取消返回 true，否则返回 false
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 获取订阅记录 ID。
     *
     * @return 订阅记录 ID
     */
    public ColumnSubscriptionId getId() {
        return id;
    }

    /**
     * 获取被订阅的专栏 ID。
     *
     * @return 专栏 ID
     */
    public ColumnId getColumnId() {
        return columnId;
    }

    /**
     * 获取订阅用户 ID。
     *
     * @return 订阅用户 ID
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * 获取订阅创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取订阅最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未取消则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
