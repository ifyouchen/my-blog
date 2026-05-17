package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.repository.LevelPrivilegeRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.LevelPrivilegeMapper;
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

    public LevelPrivilegeRepositoryImpl(LevelPrivilegeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<LevelPrivilegeConfig> findEnabledByLevel(int level) {
        return mapper.selectEnabledByLevel(level);
    }

    @Override
    public List<LevelPrivilegeConfig> findAllEnabled() {
        return mapper.selectAllEnabled();
    }
}
