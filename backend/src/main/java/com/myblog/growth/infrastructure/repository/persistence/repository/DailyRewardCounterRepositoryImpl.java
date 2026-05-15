package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.repository.DailyRewardCounterRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.DailyRewardCounterMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 每日奖励计数器 Repository 实现.
 */
@Repository
public class DailyRewardCounterRepositoryImpl implements DailyRewardCounterRepository {

    /** 每日奖励计数器 MyBatis Mapper. */
    private final DailyRewardCounterMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 每日奖励计数器 Mapper
     */
    public DailyRewardCounterRepositoryImpl(DailyRewardCounterMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     * <p>Mapper 返回 null 时（记录不存在）转为 0。</p>
     */
    @Override
    public int getGrantedExp(Long userId, String rewardType, LocalDate statDate) {
        Integer result = mapper.selectGrantedExp(userId, rewardType, statDate);
        return result == null ? 0 : result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementCounter(Long userId, String rewardType, LocalDate statDate,
                                 int countDelta, int expDelta) {
        mapper.upsertCounter(userId, rewardType, statDate, countDelta, expDelta);
    }
}

