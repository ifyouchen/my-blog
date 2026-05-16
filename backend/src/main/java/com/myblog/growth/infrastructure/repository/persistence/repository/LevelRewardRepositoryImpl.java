package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.repository.LevelRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.LevelRewardMapper;
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

    public LevelRewardRepositoryImpl(LevelRewardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<LevelRewardConfig> findByLevel(int level) {
        return mapper.selectByLevel(level);
    }

    @Override
    public List<LevelRewardConfig> findAllEnabled() {
        return mapper.selectAllEnabled();
    }
}
