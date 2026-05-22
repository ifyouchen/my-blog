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
    private final IdGenerator idGenerator;

    /**
     * 创建专栏订阅 MyBatis 仓储。
     *
     * @param columnSubscriptionMapper 专栏订阅 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisColumnSubscriptionRepository(ColumnSubscriptionMapper columnSubscriptionMapper, IdGenerator idGenerator) {
        this.columnSubscriptionMapper = columnSubscriptionMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 查询有效的专栏订阅记录（未取消）。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 专栏订阅 Optional
     */
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

    /**
     * 查询专栏订阅记录（包含已逻辑删除的记录）。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 专栏订阅 Optional
     */
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

    /**
     * 保存专栏订阅记录（新记录插入，已有记录更新）。
     *
     * @param subscription 专栏订阅聚合根
     * @return 保存后的专栏订阅聚合根
     */
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

    /**
     * 生成下一个专栏订阅记录 ID。
     *
     * @return 专栏订阅记录 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_column_subscription");
    }

    /**
     * 判断用户是否已订阅专栏。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 是否已订阅
     */
    @Override
    public boolean exists(ColumnId columnId, UserId userId) {
        return columnSubscriptionMapper.countByColumnAndUser(columnId.getValue(), userId.getValue()) > 0;
    }

    /**
     * 将 DO 转换为领域对象。
     *
     * @param columnSubscriptionDO 专栏订阅数据对象
     * @return 专栏订阅聚合根
     */
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

    /**
     * 将领域对象转换为 DO。
     *
     * @param subscription 专栏订阅聚合根
     * @return 专栏订阅数据对象
     */
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
