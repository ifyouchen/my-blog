package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 徽章定义 Repository.
 *
 * @author Codex
 * @since 1.0.0
 */
public interface BadgeDefinitionRepository {

    /**
     * 查询所有启用徽章定义.
     *
     * @return 徽章定义列表
     */
    List<BadgeDefinition> findAllEnabled();

    /**
     * 根据编码查询启用徽章.
     *
     * @param code 徽章编码
     * @return 徽章定义
     */
    Optional<BadgeDefinition> findEnabledByCode(String code);

    /**
     * 根据编码批量查询启用徽章.
     *
     * @param codes 徽章编码列表
     * @return code -> 徽章定义
     */
    Map<String, BadgeDefinition> findEnabledByCodes(List<String> codes);

    /**
     * 清理定义缓存.
     */
    void invalidateCache();
}
