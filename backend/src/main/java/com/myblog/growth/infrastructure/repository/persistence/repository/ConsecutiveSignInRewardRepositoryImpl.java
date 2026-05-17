package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import com.myblog.growth.domain.repository.ConsecutiveSignInRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ConsecutiveSignInRewardMapper;
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

    public ConsecutiveSignInRewardRepositoryImpl(ConsecutiveSignInRewardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ConsecutiveSignInRewardConfig> findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ConsecutiveSignInRewardConfig> findAllEnabled() {
        return mapper.selectAllEnabled();
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
        return mapper.updateById(config) > 0;
    }

    @Override
    public Long insert(ConsecutiveSignInRewardConfig config) {
        mapper.insert(config);
        return config.getId();
    }

    @Override
    public boolean softDelete(Long id, int version) {
        return mapper.softDelete(id, version) > 0;
    }
}
