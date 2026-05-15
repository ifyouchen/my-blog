package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserGrowthAccountDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserGrowthAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户成长账户 Repository 实现.
 * <p>
 * 通过 {@link UserGrowthAccountMapper} 与数据库交互，
 * 使用 {@link GrowthConverter} 在 DO 与领域对象之间转换。
 * </p>
 */
@Repository
public class GrowthAccountRepositoryImpl implements GrowthAccountRepository {

    /** 成长账户 MyBatis Mapper. */
    private final UserGrowthAccountMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 成长账户 Mapper
     */
    public GrowthAccountRepositoryImpl(UserGrowthAccountMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<GrowthAccount> findByUserId(Long userId) {
        UserGrowthAccountDO do_ = mapper.selectByUserId(userId);
        if (do_ == null) {
            return Optional.empty();
        }
        return Optional.of(GrowthConverter.toDomain(do_));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GrowthAccount account) {
        UserGrowthAccountDO do_ = GrowthConverter.toDO(account);
        mapper.insert(do_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int updateExpAndLevel(GrowthAccount account) {
        UserGrowthAccountDO do_ = GrowthConverter.toDO(account);
        return mapper.updateExpAndLevelCAS(do_);
    }
}

