package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.PointRule;

import java.util.List;
import java.util.Optional;

/**
 * 积分规则 Repository 接口.
 */
public interface PointRuleRepository {

    /**
     * 根据来源类型查询积分规则.
     *
     * @param sourceType 来源类型（对应 PointEventType 枚举值）
     * @return 积分规则（Optional），不存在时为 empty
     */
    Optional<PointRule> findBySourceType(String sourceType);

    /**
     * 查询所有已启用且生效的积分规则.
     *
     * @return 规则列表
     */
    List<PointRule> findAllEnabled();
}

