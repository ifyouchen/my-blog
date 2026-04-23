package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.ColumnSubscription;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ColumnSubscriptionRepository;
import com.myblog.infrastructure.repository.persistence.entity.ColumnSubscriptionDO;
import com.myblog.infrastructure.repository.persistence.mapper.ColumnSubscriptionMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 专栏订阅 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisColumnSubscriptionRepository implements ColumnSubscriptionRepository {

    private final ColumnSubscriptionMapper columnSubscriptionMapper;

    public MyBatisColumnSubscriptionRepository(ColumnSubscriptionMapper columnSubscriptionMapper) {
        this.columnSubscriptionMapper = columnSubscriptionMapper;
    }

    @Override
    public Optional<ColumnSubscription> findByColumnAndUser(ColumnId columnId, UserId userId) {
        ColumnSubscriptionDO columnSubscriptionDO = columnSubscriptionMapper.selectByColumnAndUser(
            columnId.getValue(),
            userId.getValue()
        );
        return columnSubscriptionDO == null
            ? Optional.<ColumnSubscription>empty()
            : Optional.of(toDomain(columnSubscriptionDO));
    }

    @Override
    public Optional<ColumnSubscription> findByColumnAndUserIncludingDeleted(ColumnId columnId, UserId userId) {
        ColumnSubscriptionDO columnSubscriptionDO = columnSubscriptionMapper.selectAnyByColumnAndUser(
            columnId.getValue(),
            userId.getValue()
        );
        return columnSubscriptionDO == null
            ? Optional.<ColumnSubscription>empty()
            : Optional.of(toDomain(columnSubscriptionDO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ColumnSubscription save(ColumnSubscription subscription) {
        ColumnSubscriptionDO columnSubscriptionDO = toData(subscription);
        if (columnSubscriptionMapper.selectAnyByColumnAndUser(
            subscription.getColumnId().getValue(),
            subscription.getUserId().getValue()
        ) != null) {
            columnSubscriptionMapper.update(columnSubscriptionDO);
        } else {
            columnSubscriptionMapper.insert(columnSubscriptionDO);
        }
        return subscription;
    }

    @Override
    public Long nextId() {
        Long nextId = columnSubscriptionMapper.selectNextId();
        return nextId == null ? 3000L : nextId;
    }

    @Override
    public boolean exists(ColumnId columnId, UserId userId) {
        return columnSubscriptionMapper.countByColumnAndUser(columnId.getValue(), userId.getValue()) > 0;
    }

    private ColumnSubscription toDomain(ColumnSubscriptionDO columnSubscriptionDO) {
        return ColumnSubscription.restore(
            columnSubscriptionDO.getId(),
            new ColumnId(columnSubscriptionDO.getColumnId()),
            new UserId(columnSubscriptionDO.getUserId()),
            columnSubscriptionDO.getCreatedAt(),
            columnSubscriptionDO.getUpdatedAt(),
            columnSubscriptionDO.getDeletedAt(),
            columnSubscriptionDO.getVersion()
        );
    }

    private ColumnSubscriptionDO toData(ColumnSubscription subscription) {
        ColumnSubscriptionDO columnSubscriptionDO = new ColumnSubscriptionDO();
        columnSubscriptionDO.setId(subscription.getId().getValue());
        columnSubscriptionDO.setColumnId(subscription.getColumnId().getValue());
        columnSubscriptionDO.setUserId(subscription.getUserId().getValue());
        columnSubscriptionDO.setCreatedAt(subscription.getCreatedAt());
        columnSubscriptionDO.setUpdatedAt(subscription.getUpdatedAt());
        columnSubscriptionDO.setDeletedAt(subscription.getDeletedAt());
        columnSubscriptionDO.setVersion(subscription.getVersion());
        return columnSubscriptionDO;
    }
}
