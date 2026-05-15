package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.repository.SignInRecordRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.SignInRecordMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 签到记录 Repository 实现.
 */
@Repository
public class SignInRecordRepositoryImpl implements SignInRecordRepository {

    private final SignInRecordMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 签到记录 Mapper
     */
    public SignInRecordRepositoryImpl(SignInRecordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUserIdAndDate(Long userId, LocalDate signDate) {
        return mapper.countByUserIdAndDate(userId, signDate) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(Long userId, LocalDate signDate, int continuousDays) {
        // pointsGranted 由 Application 层计算后传入，此处复用同名方法传 0，实际由调用方传入
        // 因此这里直接调 Mapper 的完整版本（含 pointsGranted 参数由 Application 层计算）
        throw new UnsupportedOperationException("请使用带 pointsGranted 参数的 insert 方法");
    }

    /**
     * 插入签到记录（含积分发放量）.
     *
     * @param userId          用户 ID
     * @param signDate        签到日期
     * @param consecutiveDays 连续签到天数
     * @param pointsGranted   本次发放积分
     * @return 插入行数（1=成功，0=已签到）
     */
    public int insertIgnore(Long userId, LocalDate signDate, int consecutiveDays, int pointsGranted) {
        return mapper.insertIgnore(userId, signDate, consecutiveDays, pointsGranted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDate findLastSignDate(Long userId) {
        return mapper.selectLastSignDate(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int findContinuousDays(Long userId) {
        Integer days = mapper.selectLastConsecutiveDays(userId);
        return days == null ? 0 : days;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LocalDate> findSignDatesByMonth(Long userId, LocalDate monthStart, LocalDate monthEnd) {
        return mapper.selectSignDatesByMonth(userId, monthStart, monthEnd);
    }
}

