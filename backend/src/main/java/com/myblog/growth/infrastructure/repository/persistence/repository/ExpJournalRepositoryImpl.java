package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.repository.ExpJournalRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserExpJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserExpJournalMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 经验流水 Repository 实现.
 */
@Repository
public class ExpJournalRepositoryImpl implements ExpJournalRepository {

    /** 经验流水 MyBatis Mapper. */
    private final UserExpJournalMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 经验流水 Mapper
     */
    public ExpJournalRepositoryImpl(UserExpJournalMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(ExpJournal journal) {
        UserExpJournalDO do_ = GrowthConverter.toDO(journal);
        return mapper.insertIgnore(do_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExpJournal> findRecentByUserId(Long userId, int limit) {
        List<UserExpJournalDO> doList = mapper.selectRecentByUserId(userId, limit);
        return doList.stream()
                .map(GrowthConverter::toDomain)
                .collect(Collectors.toList());
    }
}

