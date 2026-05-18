package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.repository.LevelRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.LevelRewardMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 等级奖励配置 Repository 实现.
 *
 * @author czx
 * @since 2026-05-17
 */
@Repository
public class LevelRewardRepositoryImpl implements LevelRewardRepository {

    private final LevelRewardMapper mapper;
    private final Cache<String, List<LevelRewardConfig>> levelRewardsCache;

    public LevelRewardRepositoryImpl(LevelRewardMapper mapper,
                                     @Qualifier("levelRewardsCache")
                                     Cache<String, List<LevelRewardConfig>> levelRewardsCache) {
        this.mapper = mapper;
        this.levelRewardsCache = levelRewardsCache;
    }

    @Override
    public Optional<LevelRewardConfig> findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Optional<LevelRewardConfig> findByLevel(int level) {
        return findAllEnabled().stream()
                .filter(item -> item.getLevel() == level)
                .findFirst();
    }

    @Override
    public Optional<LevelRewardConfig> findDeletedByLevel(int level) {
        return mapper.selectDeletedByLevel(level);
    }

    @Override
    public List<LevelRewardConfig> findAllEnabled() {
        return levelRewardsCache.get("all-enabled", key -> mapper.selectAllEnabled());
    }

    @Override
    public List<LevelRewardConfig> findAll() {
        return mapper.selectAll();
    }

    @Override
    public boolean update(LevelRewardConfig config) {
        boolean updated = mapper.updateById(config) > 0;
        if (updated) {
            levelRewardsCache.invalidateAll();
        }
        return updated;
    }

    @Override
    public Long insert(LevelRewardConfig config) {
        mapper.insert(config);
        levelRewardsCache.invalidateAll();
        return config.getId();
    }

    @Override
    public boolean restore(LevelRewardConfig config) {
        boolean restored = mapper.restoreById(config) > 0;
        if (restored) {
            levelRewardsCache.invalidateAll();
        }
        return restored;
    }

    @Override
    public boolean softDelete(Long id, int version) {
        boolean deleted = mapper.softDelete(id, version) > 0;
        if (deleted) {
            levelRewardsCache.invalidateAll();
        }
        return deleted;
    }
}
