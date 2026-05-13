package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.ColumnSubscription;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.UserId;

import java.util.Optional;

/**
 * 专栏订阅仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface ColumnSubscriptionRepository {

    /**
     * 查询当前有效的专栏订阅记录。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 订阅记录 Optional
     */
    Optional<ColumnSubscription> findByColumnAndUser(ColumnId columnId, UserId userId);

    /**
     * 查询专栏订阅记录（包含已取消/逻辑删除的记录）。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 订阅记录 Optional
     */
    Optional<ColumnSubscription> findByColumnAndUserIncludingDeleted(ColumnId columnId, UserId userId);

    /**
     * 保存专栏订阅记录（新增或更新）。
     *
     * @param subscription 订阅聚合根
     * @return 保存后的订阅记录
     */
    ColumnSubscription save(ColumnSubscription subscription);

    /**
     * 生成下一个订阅记录 ID。
     *
     * @return 订阅记录 ID
     */
    Long nextId();

    /**
     * 判断用户是否已订阅专栏（仅有效记录）。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 是否已订阅
     */
    boolean exists(ColumnId columnId, UserId userId);
}
