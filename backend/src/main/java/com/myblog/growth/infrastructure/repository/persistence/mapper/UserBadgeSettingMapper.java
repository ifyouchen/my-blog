package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.UserBadgeSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户徽章设置 MyBatis Mapper.
 *
 * @author Codex
 * @since 1.0.0
 */
@Mapper
public interface UserBadgeSettingMapper {

    /**
     * 查询用户徽章设置.
     *
     * @param userId 用户ID
     * @return 设置
     */
    Optional<UserBadgeSetting> selectByUserId(@Param("userId") Long userId);

    /**
     * 插入或更新佩戴徽章.
     *
     * @param userId 用户ID
     * @param badgeCode 徽章编码
     * @return 影响行数
     */
    int upsertEquipped(@Param("userId") Long userId, @Param("badgeCode") String badgeCode);

    /**
     * 批量查询已佩戴徽章.
     *
     * @param userIds 用户ID列表
     * @return 行列表
     */
    List<EquippedBadgeRow> selectEquippedDefinitionsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 已佩戴徽章行.
     *
     * @author Codex
     * @since 1.0.0
     */
    class EquippedBadgeRow {
        private Long userId;
        private BadgeDefinition badge;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public BadgeDefinition getBadge() { return badge; }
        public void setBadge(BadgeDefinition badge) { this.badge = badge; }
    }
}
