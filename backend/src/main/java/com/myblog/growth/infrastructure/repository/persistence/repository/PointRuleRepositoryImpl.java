package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.repository.PointRuleRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.PointRuleConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.PointRuleConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 积分规则 Repository 实现.
 * <p>
 * 通过 {@link PointRuleConfigMapper} 与数据库交互，
 * 使用 {@link GrowthConverter} 在 DO 与领域对象之间转换。
 * </p>
 */
@Repository
public class PointRuleRepositoryImpl implements PointRuleRepository {

    /** 积分规则配置 MyBatis Mapper. */
    private final PointRuleConfigMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 积分规则配置 Mapper
     */
    public PointRuleRepositoryImpl(PointRuleConfigMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PointRule> findBySourceType(String sourceType) {
        PointRuleConfigDO do_ = mapper.selectBySourceType(sourceType);
        if (do_ == null) {
            return Optional.empty();
        }
        return Optional.of(GrowthConverter.toPointRuleDomain(do_));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointRule> findAllEnabled() {
        List<PointRuleConfigDO> doList = mapper.selectAllEnabled();
        return doList.stream()
                .map(GrowthConverter::toPointRuleDomain)
                .collect(Collectors.toList());
    }
}

