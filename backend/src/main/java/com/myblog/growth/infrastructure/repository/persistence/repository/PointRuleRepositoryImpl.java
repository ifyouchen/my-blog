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

    private final PointRuleConfigMapper mapper;

    public PointRuleRepositoryImpl(PointRuleConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<PointRule> findBySourceType(String sourceType) {
        PointRuleConfigDO do_ = mapper.selectBySourceType(sourceType);
        return do_ == null ? Optional.empty() : Optional.of(GrowthConverter.toPointRuleDomain(do_));
    }

    @Override
    public List<PointRule> findAllEnabled() {
        return mapper.selectAllEnabled().stream()
                .map(GrowthConverter::toPointRuleDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PointRule> findAllForAdmin() {
        return mapper.selectAllForAdmin().stream()
                .map(GrowthConverter::toPointRuleDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PointRule> findById(Long id) {
        PointRuleConfigDO do_ = mapper.selectById(id);
        return do_ == null ? Optional.empty() : Optional.of(GrowthConverter.toPointRuleDomain(do_));
    }

    @Override
    public Optional<PointRule> findBySourceTypeIncludingDeleted(String sourceType) {
        PointRuleConfigDO do_ = mapper.selectBySourceTypeIncludingDeleted(sourceType);
        return do_ == null ? Optional.empty() : Optional.of(GrowthConverter.toPointRuleDomain(do_));
    }

    @Override
    public Optional<PointRule> findDeletedBySourceType(String sourceType) {
        PointRuleConfigDO do_ = mapper.selectDeletedBySourceType(sourceType);
        return do_ == null ? Optional.empty() : Optional.of(GrowthConverter.toPointRuleDomain(do_));
    }

    @Override
    public Long save(PointRule rule) {
        PointRuleConfigDO do_ = GrowthConverter.toPointRuleDO(rule);
        mapper.insert(do_);
        return do_.getId();
    }

    @Override
    public boolean update(PointRule rule) {
        PointRuleConfigDO do_ = GrowthConverter.toPointRuleDO(rule);
        return mapper.updateCAS(do_) > 0;
    }

    @Override
    public boolean softDelete(Long id, int version, String operator, String reason) {
        return mapper.softDeleteCAS(id, version, operator, reason) > 0;
    }

    @Override
    public boolean restore(PointRule rule) {
        PointRuleConfigDO do_ = GrowthConverter.toPointRuleDO(rule);
        return mapper.restoreCAS(do_) > 0;
    }
}
