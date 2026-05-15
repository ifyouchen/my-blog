package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.aggregate.PointAccount;
import com.myblog.growth.domain.repository.PointAccountRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserPointAccountDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserPointAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 积分账户 Repository 实现.
 * <p>
 * 通过 {@link UserPointAccountMapper} 与数据库交互，
 * 使用 {@link GrowthConverter} 在 DO 与领域对象之间转换。
 * </p>
 */
@Repository
public class PointAccountRepositoryImpl implements PointAccountRepository {

    /** 积分账户 MyBatis Mapper. */
    private final UserPointAccountMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 积分账户 Mapper
     */
    public PointAccountRepositoryImpl(UserPointAccountMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PointAccount> findByUserId(Long userId) {
        UserPointAccountDO do_ = mapper.selectByUserId(userId);
        if (do_ == null) {
            return Optional.empty();
        }
        return Optional.of(GrowthConverter.toPointAccountDomain(do_));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(PointAccount account) {
        UserPointAccountDO do_ = GrowthConverter.toPointAccountDO(account);
        mapper.insert(do_);
        account.setId(do_.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int updateCAS(PointAccount account) {
        UserPointAccountDO do_ = GrowthConverter.toPointAccountDO(account);
        return mapper.updateBalanceCAS(do_);
    }
}

