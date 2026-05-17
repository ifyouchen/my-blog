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
    public Optional<LevelRewardConfig> findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Optional<LevelRewardConfig> findByLevel(int level) {
        return mapper.selectByLevel(level);
    }

    @Override
    public Optional<LevelRewardConfig> findDeletedByLevel(int level) {
        return mapper.selectDeletedByLevel(level);
    }

    @Override
    public List<LevelRewardConfig> findAllEnabled() {
        return mapper.selectAllEnabled();
    }

    @Override
    public List<LevelRewardConfig> findAll() {
        return mapper.selectAll();
    }

    @Override
    public boolean update(LevelRewardConfig config) {
        return mapper.updateById(config) > 0;
    }

    @Override
    public Long insert(LevelRewardConfig config) {
        mapper.insert(config);
        return config.getId();
    }

    @Override
    public boolean restore(LevelRewardConfig config) {
        return mapper.restoreById(config) > 0;
    }

    @Override
    public boolean softDelete(Long id, int version) {
        return mapper.softDelete(id, version) > 0;
    }
}
