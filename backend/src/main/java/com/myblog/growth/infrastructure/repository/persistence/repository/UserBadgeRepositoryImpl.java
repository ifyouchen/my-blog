package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.UserBadge;
import com.myblog.growth.domain.repository.UserBadgeRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserBadgeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户徽章 Repository 实现.
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class UserBadgeRepositoryImpl implements UserBadgeRepository {

    private final UserBadgeMapper mapper;

    public UserBadgeRepositoryImpl(UserBadgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int insertIgnore(UserBadge badge) {
        return mapper.insertIgnore(badge);
    }

    @Override
    public boolean existsByUserIdAndBadgeCode(Long userId, String badgeCode) {
        return userId != null && badgeCode != null && mapper.countByUserIdAndBadgeCode(userId, badgeCode) > 0;
    }

    @Override
    public List<String> findCodesByUserId(Long userId) {
        return mapper.selectCodesByUserId(userId);
    }

    @Override
    public List<BadgeDefinition> findOwnedDefinitions(Long userId) {
        return mapper.selectOwnedDefinitions(userId);
    }
}
