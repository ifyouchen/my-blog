package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.domain.repository.PointJournalRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserPointJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserPointJournalMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 积分流水 Repository 实现.
 * <p>
 * 通过 {@link UserPointJournalMapper} 与数据库交互，
 * 使用 {@link GrowthConverter} 在 DO 与领域对象之间转换。
 * 流水表只允许 INSERT，禁止 UPDATE 和物理 DELETE。
 * </p>
 */
@Repository
public class PointJournalRepositoryImpl implements PointJournalRepository {

    /** 积分流水 MyBatis Mapper. */
    private final UserPointJournalMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 积分流水 Mapper
     */
    public PointJournalRepositoryImpl(UserPointJournalMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(PointJournal journal) {
        UserPointJournalDO do_ = GrowthConverter.toPointJournalDO(journal);
        return mapper.insertIgnore(do_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointJournal> findRecentByUserId(Long userId, int limit) {
        List<UserPointJournalDO> doList = mapper.selectRecentByUserId(userId, limit);
        return doList.stream()
                .map(GrowthConverter::toPointJournalDomain)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointJournal> findPageByUserId(Long userId, String sourceType, int page, int size) {
        int offset = (page - 1) * size;
        List<UserPointJournalDO> doList = mapper.selectPageByUserIdAndSourceType(userId, sourceType, size, offset);
        return doList.stream()
                .map(GrowthConverter::toPointJournalDomain)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByUserId(Long userId, String sourceType) {
        return mapper.countByUserIdAndSourceType(userId, sourceType);
    }
}

