package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.UserSignInStats;
import com.myblog.growth.domain.repository.UserSignInStatsRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserSignInStatsMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户签到统计 Repository 实现.
 *
 * @author czx
 * @since 2026-05-17
 */
@Repository
public class UserSignInStatsRepositoryImpl implements UserSignInStatsRepository {

    private final UserSignInStatsMapper mapper;

    public UserSignInStatsRepositoryImpl(UserSignInStatsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserSignInStats> findByUserId(Long userId) {
        return mapper.selectByUserId(userId);
    }

    @Override
    public void save(UserSignInStats stats) {
        mapper.insert(stats);
    }

    @Override
    public int updateWithCAS(UserSignInStats stats) {
        return mapper.updateWithCAS(stats);
    }
}
