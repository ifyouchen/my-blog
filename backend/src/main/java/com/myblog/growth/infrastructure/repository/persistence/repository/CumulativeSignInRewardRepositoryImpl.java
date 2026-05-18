package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.repository.CumulativeSignInRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.CumulativeSignInRewardMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 累计签到里程碑奖励配置 Repository 实现.
 *
 * @author czx
 * @since 2026-05-17
 */
@Repository
public class CumulativeSignInRewardRepositoryImpl implements CumulativeSignInRewardRepository {

    private final CumulativeSignInRewardMapper mapper;
    private final Cache<String, List<CumulativeSignInRewardConfig>> cumulativeSignInRewardsCache;

    public CumulativeSignInRewardRepositoryImpl(CumulativeSignInRewardMapper mapper,
                                                @Qualifier("cumulativeSignInRewardsCache")
                                                Cache<String, List<CumulativeSignInRewardConfig>> cumulativeSignInRewardsCache) {
        this.mapper = mapper;
        this.cumulativeSignInRewardsCache = cumulativeSignInRewardsCache;
    }

    @Override
    public Optional<CumulativeSignInRewardConfig> findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<CumulativeSignInRewardConfig> findAllEnabled() {
        return cumulativeSignInRewardsCache.get("all-enabled", key -> mapper.selectAllEnabled());
    }

    @Override
    public List<CumulativeSignInRewardConfig> findByMilestoneLessThanEqual(int days) {
        return mapper.selectByMilestoneLessThanEqual(days);
    }

    @Override
    public Optional<CumulativeSignInRewardConfig> findDeletedByMilestoneDays(int milestoneDays) {
        return mapper.selectDeletedByMilestoneDays(milestoneDays);
    }

    @Override
    public List<CumulativeSignInRewardConfig> findAll() {
        return mapper.selectAll();
    }

    @Override
    public boolean update(CumulativeSignInRewardConfig config) {
        boolean updated = mapper.updateById(config) > 0;
        if (updated) {
            cumulativeSignInRewardsCache.invalidateAll();
        }
        return updated;
    }

    @Override
    public Long insert(CumulativeSignInRewardConfig config) {
        mapper.insert(config);
        cumulativeSignInRewardsCache.invalidateAll();
        return config.getId();
    }

    @Override
    public boolean restore(CumulativeSignInRewardConfig config) {
        boolean restored = mapper.restoreById(config) > 0;
        if (restored) {
            cumulativeSignInRewardsCache.invalidateAll();
        }
        return restored;
    }

    @Override
    public boolean softDelete(Long id, int version) {
        boolean deleted = mapper.softDelete(id, version) > 0;
        if (deleted) {
            cumulativeSignInRewardsCache.invalidateAll();
        }
        return deleted;
    }
}
