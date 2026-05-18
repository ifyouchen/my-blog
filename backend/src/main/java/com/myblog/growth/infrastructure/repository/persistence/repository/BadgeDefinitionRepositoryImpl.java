package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.repository.BadgeDefinitionRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.BadgeDefinitionMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 徽章定义 Repository 实现.
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class BadgeDefinitionRepositoryImpl implements BadgeDefinitionRepository {

    private static final String ALL_ENABLED_KEY = "all-enabled";

    private final BadgeDefinitionMapper mapper;
    private final Cache<String, List<BadgeDefinition>> badgeDefinitionsCache;

    public BadgeDefinitionRepositoryImpl(BadgeDefinitionMapper mapper,
                                         @Qualifier("badgeDefinitionsCache")
                                         Cache<String, List<BadgeDefinition>> badgeDefinitionsCache) {
        this.mapper = mapper;
        this.badgeDefinitionsCache = badgeDefinitionsCache;
    }

    @Override
    public List<BadgeDefinition> findAllEnabled() {
        return badgeDefinitionsCache.get(ALL_ENABLED_KEY, key -> mapper.selectAllEnabled());
    }

    @Override
    public Optional<BadgeDefinition> findEnabledByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        for (BadgeDefinition definition : findAllEnabled()) {
            if (code.trim().equals(definition.getCode())) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, BadgeDefinition> findEnabledByCodes(List<String> codes) {
        Map<String, BadgeDefinition> result = new HashMap<String, BadgeDefinition>();
        if (codes == null || codes.isEmpty()) {
            return result;
        }
        for (BadgeDefinition definition : findAllEnabled()) {
            if (codes.contains(definition.getCode())) {
                result.put(definition.getCode(), definition);
            }
        }
        return result;
    }

    @Override
    public void invalidateCache() {
        badgeDefinitionsCache.invalidateAll();
    }
}
