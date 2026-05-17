package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.repository.CumulativeSignInRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.CumulativeSignInRewardMapper;
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

    public CumulativeSignInRewardRepositoryImpl(CumulativeSignInRewardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<CumulativeSignInRewardConfig> findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<CumulativeSignInRewardConfig> findAllEnabled() {
        return mapper.selectAllEnabled();
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
        return mapper.updateById(config) > 0;
    }

    @Override
    public Long insert(CumulativeSignInRewardConfig config) {
        mapper.insert(config);
        return config.getId();
    }

    @Override
    public boolean restore(CumulativeSignInRewardConfig config) {
        return mapper.restoreById(config) > 0;
    }

    @Override
    public boolean softDelete(Long id, int version) {
        return mapper.softDelete(id, version) > 0;
    }
}
