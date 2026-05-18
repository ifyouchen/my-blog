package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.UserBadgeSetting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户徽章佩戴设置 Repository.
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserBadgeSettingRepository {

    /**
     * 查询用户徽章佩戴设置.
     *
     * @param userId 用户ID
     * @return 佩戴设置
     */
    Optional<UserBadgeSetting> findByUserId(Long userId);

    /**
     * 更新用户当前佩戴徽章.
     *
     * @param userId 用户ID
     * @param badgeCode 徽章编码，可为空
     */
    void upsertEquipped(Long userId, String badgeCode);

    /**
     * 批量查询用户当前佩戴的启用徽章.
     *
     * @param userIds 用户ID列表
     * @return userId -> 徽章定义
     */
    Map<Long, BadgeDefinition> findEquippedDefinitionsByUserIds(List<Long> userIds);
}
