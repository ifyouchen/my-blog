package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.PointRule;

import java.util.List;
import java.util.Optional;

/**
 * 积分规则 Repository 接口.
 */
public interface PointRuleRepository {

    /**
     * 根据行为类型查询积分规则.
     *
     * @param eventType 行为类型
     * @return 积分规则（Optional），不存在时为 empty
     */
    Optional<PointRule> findByEventType(String eventType);

    /**
     * 查询所有已启用且生效的积分规则.
     *
     * @return 规则列表
     */
    List<PointRule> findAllEnabled();
}

