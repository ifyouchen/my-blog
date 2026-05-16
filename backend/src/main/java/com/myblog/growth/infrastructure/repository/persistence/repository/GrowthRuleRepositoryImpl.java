package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.repository.GrowthRuleRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.GrowthRuleConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.GrowthRuleConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 经验规则 Repository 实现.
 */
@Repository
public class GrowthRuleRepositoryImpl implements GrowthRuleRepository {

    /** 经验规则配置 MyBatis Mapper. */
    private final GrowthRuleConfigMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 经验规则配置 Mapper
     */
    public GrowthRuleRepositoryImpl(GrowthRuleConfigMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<GrowthRule> findByEventTypeAndRole(String eventType, String role) {
        GrowthRuleConfigDO do_ = mapper.selectByEventTypeAndRole(eventType, role);
        if (do_ == null) {
            return Optional.empty();
        }
        return Optional.of(GrowthConverter.toDomain(do_));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GrowthRule> findAllEnabled() {
        return mapper.selectAllEnabled().stream()
                .map(GrowthConverter::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long insert(GrowthRule rule) {
        GrowthRuleConfigDO do_ = GrowthConverter.toDO(rule);
        mapper.insert(do_);
        return do_.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(GrowthRule rule) {
        GrowthRuleConfigDO do_ = GrowthConverter.toDO(rule);
        return mapper.updateCAS(do_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int softDelete(Long id, int version, String operator, String reason) {
        return mapper.softDeleteCAS(id, version, operator, reason);
    }
}

