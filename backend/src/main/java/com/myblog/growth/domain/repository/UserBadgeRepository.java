package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.UserBadge;

import java.util.List;

/**
 * 用户徽章 Repository.
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserBadgeRepository {

    /**
     * 幂等授予徽章.
     *
     * @param badge 用户徽章
     * @return 插入行数，已拥有时为 0
     */
    int insertIgnore(UserBadge badge);

    /**
     * 判断用户是否拥有徽章.
     *
     * @param userId 用户ID
     * @param badgeCode 徽章编码
     * @return true 表示已拥有
     */
    boolean existsByUserIdAndBadgeCode(Long userId, String badgeCode);

    /**
     * 查询用户已拥有徽章编码.
     *
     * @param userId 用户ID
     * @return 编码列表
     */
    List<String> findCodesByUserId(Long userId);

    /**
     * 查询用户已拥有的启用徽章定义.
     *
     * @param userId 用户ID
     * @return 徽章定义列表
     */
    List<BadgeDefinition> findOwnedDefinitions(Long userId);
}
