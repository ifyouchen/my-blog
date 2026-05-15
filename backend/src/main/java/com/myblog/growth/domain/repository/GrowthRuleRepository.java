package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.GrowthRule;

import java.util.List;
import java.util.Optional;

/**
 * 经验规则 Repository 接口.
 */
public interface GrowthRuleRepository {

    /**
     * 根据事件类型和角色查询规则.
     *
     * @param eventType 事件类型
     * @param role      角色
     * @return 规则（Optional）
     */
    Optional<GrowthRule> findByEventTypeAndRole(String eventType, String role);

    /**
     * 查询所有启用的规则.
     *
     * @return 规则列表
     */
    List<GrowthRule> findAllEnabled();

    /**
     * 新增规则.
     *
     * @param rule 规则
     * @return 规则 ID
     */
    Long insert(GrowthRule rule);

    /**
     * 更新规则.
     *
     * @param rule 规则
     * @return 更新行数
     */
    int update(GrowthRule rule);
}

