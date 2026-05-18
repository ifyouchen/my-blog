package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import com.myblog.growth.domain.repository.ConsecutiveSignInRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ConsecutiveSignInRewardMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 连续签到奖励配置 Repository 实现.
 *
 * @author czx
 * @since 2026-05-17
 */
@Repository
public class ConsecutiveSignInRewardRepositoryImpl implements ConsecutiveSignInRewardRepository {

    private final ConsecutiveSignInRewardMapper mapper;
    private final Cache<String, List<ConsecutiveSignInRewardConfig>> consecutiveSignInRewardsCache;

    public ConsecutiveSignInRewardRepositoryImpl(ConsecutiveSignInRewardMapper mapper,
                                                 @Qualifier("consecutiveSignInRewardsCache")
                                                 Cache<String, List<ConsecutiveSignInRewardConfig>> consecutiveSignInRewardsCache) {
        this.mapper = mapper;
        this.consecutiveSignInRewardsCache = consecutiveSignInRewardsCache;
    }

    @Override
    public Optional<ConsecutiveSignInRewardConfig> findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ConsecutiveSignInRewardConfig> findAllEnabled() {
        return consecutiveSignInRewardsCache.get("all-enabled", key -> mapper.selectAllEnabled());
    }

    @Override
    public Optional<ConsecutiveSignInRewardConfig> findByConsecutiveDays(int consecutiveDays) {
        return mapper.selectByConsecutiveDays(consecutiveDays);
    }

    @Override
    public List<ConsecutiveSignInRewardConfig> findAll() {
        return mapper.selectAll();
    }

    @Override
    public boolean update(ConsecutiveSignInRewardConfig config) {
        boolean updated = mapper.updateById(config) > 0;
        if (updated) {
            consecutiveSignInRewardsCache.invalidateAll();
        }
        return updated;
    }

    @Override
    public Long insert(ConsecutiveSignInRewardConfig config) {
        mapper.insert(config);
        consecutiveSignInRewardsCache.invalidateAll();
        return config.getId();
    }

    @Override
    public boolean softDelete(Long id, int version) {
        boolean deleted = mapper.softDelete(id, version) > 0;
        if (deleted) {
            consecutiveSignInRewardsCache.invalidateAll();
        }
        return deleted;
    }
}
