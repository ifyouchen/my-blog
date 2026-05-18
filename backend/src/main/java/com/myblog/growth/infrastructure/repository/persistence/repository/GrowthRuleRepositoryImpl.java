package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.repository.GrowthRuleRepository;
import com.myblog.growth.infrastructure.repository.persistence.converter.GrowthConverter;
import com.myblog.growth.infrastructure.repository.persistence.entity.GrowthRuleConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.GrowthRuleConfigMapper;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final Cache<String, List<GrowthRule>> growthRulesCache;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 经验规则配置 Mapper
     */
    public GrowthRuleRepositoryImpl(GrowthRuleConfigMapper mapper,
                                    @Qualifier("growthRulesCache")
                                    Cache<String, List<GrowthRule>> growthRulesCache) {
        this.mapper = mapper;
        this.growthRulesCache = growthRulesCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<GrowthRule> findByEventTypeAndRole(String eventType, String role) {
        return findAllEnabled().stream()
                .filter(rule -> eventType.equals(rule.getEventType()) && role.equals(rule.getRole()))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GrowthRule> findAllEnabled() {
        return growthRulesCache.get("all-enabled", key -> mapper.selectAllEnabled().stream()
                .map(GrowthConverter::toDomain)
                .collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long insert(GrowthRule rule) {
        GrowthRuleConfigDO do_ = GrowthConverter.toDO(rule);
        mapper.insert(do_);
        growthRulesCache.invalidateAll();
        return do_.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(GrowthRule rule) {
        GrowthRuleConfigDO do_ = GrowthConverter.toDO(rule);
        int updated = mapper.updateCAS(do_);
        if (updated > 0) {
            growthRulesCache.invalidateAll();
        }
        return updated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int softDelete(Long id, int version, String operator, String reason) {
        int deleted = mapper.softDeleteCAS(id, version, operator, reason);
        if (deleted > 0) {
            growthRulesCache.invalidateAll();
        }
        return deleted;
    }
}

