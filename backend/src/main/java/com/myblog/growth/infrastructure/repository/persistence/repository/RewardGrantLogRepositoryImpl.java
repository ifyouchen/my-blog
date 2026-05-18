package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.repository.RewardGrantLogRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.RewardGrantLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户奖励领取记录 Repository 实现.
 *
 * @author czx
 * @since 2026-05-17
 */
@Repository
public class RewardGrantLogRepositoryImpl implements RewardGrantLogRepository {

    private final RewardGrantLogMapper mapper;

    public RewardGrantLogRepositoryImpl(RewardGrantLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsByUserAndReward(Long userId, String rewardType, Long rewardId) {
        return mapper.countByUserAndReward(userId, rewardType, rewardId) > 0;
    }

    @Override
    public void save(RewardGrantLog log) {
        mapper.insert(log);
    }

    @Override
    public List<RewardGrantLog> findByUserId(Long userId) {
        return mapper.selectByUserId(userId);
    }

    @Override
    public long countForAdmin(Long userId, String userKeyword, String rewardType) {
        return mapper.countForAdmin(userId, userKeyword, rewardType);
    }

    @Override
    public List<RewardGrantLog> findPageForAdmin(Long userId,
                                                 String userKeyword,
                                                 String rewardType,
                                                 int offset,
                                                 int limit) {
        return mapper.selectPageForAdmin(userId, userKeyword, rewardType, offset, limit);
    }
}
