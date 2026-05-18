package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.repository.LevelPrivilegeRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.LevelPrivilegeMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 等级权益配置仓储实现.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Repository
public class LevelPrivilegeRepositoryImpl implements LevelPrivilegeRepository {

    private final LevelPrivilegeMapper mapper;
    private final Cache<String, List<LevelPrivilegeConfig>> levelPrivilegesCache;

    public LevelPrivilegeRepositoryImpl(LevelPrivilegeMapper mapper,
                                        @Qualifier("levelPrivilegesCache")
                                        Cache<String, List<LevelPrivilegeConfig>> levelPrivilegesCache) {
        this.mapper = mapper;
        this.levelPrivilegesCache = levelPrivilegesCache;
    }

    @Override
    public List<LevelPrivilegeConfig> findEnabledByLevel(int level) {
        List<LevelPrivilegeConfig> result = new java.util.ArrayList<LevelPrivilegeConfig>();
        for (LevelPrivilegeConfig config : findAllEnabled()) {
            if (config.getLevel() == level) {
                result.add(config);
            }
        }
        return result;
    }

    @Override
    public List<LevelPrivilegeConfig> findAllEnabled() {
        return levelPrivilegesCache.get("all-enabled", key -> mapper.selectAllEnabled());
    }
}
