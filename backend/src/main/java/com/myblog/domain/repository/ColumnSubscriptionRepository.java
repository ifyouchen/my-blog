package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.ColumnSubscription;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.UserId;

import java.util.Optional;

/**
 * 专栏订阅仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ColumnSubscriptionRepository {

    Optional<ColumnSubscription> findByColumnAndUser(ColumnId columnId, UserId userId);

    Optional<ColumnSubscription> findByColumnAndUserIncludingDeleted(ColumnId columnId, UserId userId);

    ColumnSubscription save(ColumnSubscription subscription);

    Long nextId();

    boolean exists(ColumnId columnId, UserId userId);
}
