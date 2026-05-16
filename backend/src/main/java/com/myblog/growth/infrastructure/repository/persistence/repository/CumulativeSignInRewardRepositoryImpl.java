package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.repository.CumulativeSignInRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.CumulativeSignInRewardMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    public List<CumulativeSignInRewardConfig> findAllEnabled() {
        return mapper.selectAllEnabled();
    }

    @Override
    public List<CumulativeSignInRewardConfig> findByMilestoneLessThanEqual(int days) {
        return mapper.selectByMilestoneLessThanEqual(days);
    }
}
