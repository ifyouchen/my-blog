package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.UserBadgeSetting;
import com.myblog.growth.domain.repository.UserBadgeSettingRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserBadgeSettingMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户徽章设置 Repository 实现.
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class UserBadgeSettingRepositoryImpl implements UserBadgeSettingRepository {

    private final UserBadgeSettingMapper mapper;

    public UserBadgeSettingRepositoryImpl(UserBadgeSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserBadgeSetting> findByUserId(Long userId) {
        return mapper.selectByUserId(userId);
    }

    @Override
    public void upsertEquipped(Long userId, String badgeCode) {
        mapper.upsertEquipped(userId, badgeCode);
    }

    @Override
    public Map<Long, BadgeDefinition> findEquippedDefinitionsByUserIds(List<Long> userIds) {
        Map<Long, BadgeDefinition> result = new HashMap<Long, BadgeDefinition>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        for (UserBadgeSettingMapper.EquippedBadgeRow row : mapper.selectEquippedDefinitionsByUserIds(userIds)) {
            if (row.getUserId() != null && row.getBadge() != null) {
                result.put(row.getUserId(), row.getBadge());
            }
        }
        return result;
    }
}
