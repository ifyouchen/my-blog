package com.myblog.growth.application.service;

import com.myblog.growth.application.dto.MyBadgesDTO;
import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.UserBadge;
import com.myblog.growth.domain.model.valueobject.UserBadgeSetting;
import com.myblog.growth.domain.repository.BadgeDefinitionRepository;
import com.myblog.growth.domain.repository.UserBadgeRepository;
import com.myblog.growth.domain.repository.UserBadgeSettingRepository;
import com.myblog.growth.shared.constant.BadgeCodes;
import com.myblog.growth.shared.constant.GrowthPrivilegeCodes;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.shared.dto.BadgeDisplayDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 徽章应用服务.
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class BadgeAppService {

    private static final String SOURCE_LEVEL = "LEVEL";
    private static final String SOURCE_SIGN = "SIGN_IN";
    private static final String SOURCE_PRIVILEGE = "PRIVILEGE";
    private static final int MAX_LEVEL_BADGE = 10;
    private static final int[] SIGN_MILESTONES = new int[] {7, 30, 100, 200, 365};

    private final BadgeDefinitionRepository badgeDefinitionRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserBadgeSettingRepository userBadgeSettingRepository;

    public BadgeAppService(BadgeDefinitionRepository badgeDefinitionRepository,
                           UserBadgeRepository userBadgeRepository,
                           UserBadgeSettingRepository userBadgeSettingRepository) {
        this.badgeDefinitionRepository = badgeDefinitionRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userBadgeSettingRepository = userBadgeSettingRepository;
    }

    /**
     * 查询我的徽章.
     *
     * @param userId 用户ID
     * @return 我的徽章信息
     */
    public MyBadgesDTO listMyBadges(Long userId) {
        List<BadgeDefinition> allDefinitions = badgeDefinitionRepository.findAllEnabled();
        Set<String> ownedCodes = new HashSet<String>(userBadgeRepository.findCodesByUserId(userId));
        String equippedCode = userBadgeSettingRepository.findByUserId(userId)
                .map(UserBadgeSetting::getEquippedBadgeCode)
                .orElse(null);

        List<BadgeDisplayDTO> badges = new ArrayList<BadgeDisplayDTO>(allDefinitions.size());
        BadgeDisplayDTO equipped = null;
        for (BadgeDefinition definition : allDefinitions) {
            boolean owned = ownedCodes.contains(definition.getCode());
            boolean isEquipped = owned && definition.getCode().equals(equippedCode);
            BadgeDisplayDTO dto = toDisplayDTO(definition, owned, isEquipped);
            badges.add(dto);
            if (isEquipped) {
                equipped = dto;
            }
        }
        MyBadgesDTO result = new MyBadgesDTO();
        result.setBadges(badges);
        result.setEquippedBadge(equipped);
        return result;
    }

    /**
     * 设置当前佩戴徽章.
     *
     * @param userId 用户ID
     * @param badgeCode 徽章编码，可为空
     * @return 更新后的我的徽章信息
     */
    @Transactional(rollbackFor = Exception.class)
    public MyBadgesDTO equipBadge(Long userId, String badgeCode) {
        String normalizedCode = trimToNull(badgeCode);
        if (normalizedCode != null) {
            badgeDefinitionRepository.findEnabledByCode(normalizedCode)
                    .orElseThrow(() -> new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "徽章不存在或已停用"));
            if (!userBadgeRepository.existsByUserIdAndBadgeCode(userId, normalizedCode)) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "只能佩戴已拥有的徽章");
            }
        }
        userBadgeSettingRepository.upsertEquipped(userId, normalizedCode);
        return listMyBadges(userId);
    }

    /**
     * 查询多个用户当前佩戴徽章.
     *
     * @param userIds 用户ID列表
     * @return userId -> 徽章展示 DTO
     */
    public Map<Long, BadgeDisplayDTO> findEquippedBadges(List<Long> userIds) {
        Map<Long, BadgeDefinition> definitionMap =
                userBadgeSettingRepository.findEquippedDefinitionsByUserIds(userIds);
        java.util.Map<Long, BadgeDisplayDTO> result = new java.util.HashMap<Long, BadgeDisplayDTO>();
        for (Map.Entry<Long, BadgeDefinition> entry : definitionMap.entrySet()) {
            result.put(entry.getKey(), toDisplayDTO(entry.getValue(), true, true));
        }
        return result;
    }

    /**
     * 授予等级徽章，跳级时补齐中间等级.
     *
     * @param userId 用户ID
     * @param targetLevel 目标等级
     * @return 本次新授予数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int grantLevelBadges(Long userId, int targetLevel) {
        int granted = 0;
        int safeLevel = Math.min(Math.max(targetLevel, 1), MAX_LEVEL_BADGE);
        for (int level = 1; level <= safeLevel; level++) {
            granted += grantBadge(userId, BadgeCodes.level(level), SOURCE_LEVEL, Long.valueOf(level));
        }
        return granted;
    }

    /**
     * 按累计签到天数授予里程碑徽章.
     *
     * @param userId 用户ID
     * @param totalSignDays 累计签到天数
     * @return 本次新授予数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int grantSignInBadges(Long userId, int totalSignDays) {
        int granted = 0;
        for (int milestone : SIGN_MILESTONES) {
            if (totalSignDays >= milestone) {
                granted += grantBadge(userId, BadgeCodes.sign(milestone), SOURCE_SIGN, Long.valueOf(milestone));
            }
        }
        return granted;
    }

    /**
     * 按指定徽章编码授予累计签到徽章.
     *
     * @param userId 用户ID
     * @param badgeCode 徽章编码
     * @param milestoneDays 里程碑天数
     * @return 是否新授予
     */
    @Transactional(rollbackFor = Exception.class)
    public int grantSignInBadge(Long userId, String badgeCode, int milestoneDays) {
        String normalizedCode = trimToNull(badgeCode);
        if (normalizedCode == null) {
            normalizedCode = BadgeCodes.sign(milestoneDays);
        }
        return grantBadge(userId, normalizedCode, SOURCE_SIGN, Long.valueOf(milestoneDays));
    }

    /**
     * 授予年度创作者候选徽章.
     *
     * @param userId 用户ID
     * @return 是否新授予
     */
    @Transactional(rollbackFor = Exception.class)
    public int grantAnnualCreatorCandidateBadge(Long userId) {
        return grantBadge(userId, BadgeCodes.ANNUAL_CREATOR_CANDIDATE, SOURCE_PRIVILEGE, null);
    }

    /**
     * 补偿用户徽章.
     *
     * @param userId 用户ID
     * @param currentLevel 当前等级
     * @param totalSignDays 累计签到天数
     * @param privilegeCodes 权益编码列表
     * @return 本次新授予数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int backfillBadges(Long userId, int currentLevel, int totalSignDays, List<String> privilegeCodes) {
        int granted = grantLevelBadges(userId, currentLevel) + grantSignInBadges(userId, totalSignDays);
        if (privilegeCodes != null && privilegeCodes.contains(GrowthPrivilegeCodes.ANNUAL_CREATOR_ELIGIBLE)) {
            granted += grantAnnualCreatorCandidateBadge(userId);
        }
        return granted;
    }

    private int grantBadge(Long userId, String badgeCode, String sourceType, Long sourceId) {
        if (userId == null || badgeCode == null || !badgeDefinitionRepository.findEnabledByCode(badgeCode).isPresent()) {
            return 0;
        }
        return userBadgeRepository.insertIgnore(UserBadge.grant(userId, badgeCode, sourceType, sourceId));
    }

    private BadgeDisplayDTO toDisplayDTO(BadgeDefinition definition, boolean owned, boolean equipped) {
        BadgeDisplayDTO dto = new BadgeDisplayDTO();
        dto.setCode(definition.getCode());
        dto.setType(definition.getType());
        dto.setName(definition.getName());
        dto.setDescription(definition.getDescription());
        dto.setIconKey(definition.getIconKey());
        dto.setTone(definition.getTone());
        dto.setRarity(definition.getRarity());
        dto.setSortOrder(definition.getSortOrder());
        dto.setOwned(owned);
        dto.setEquipped(equipped);
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
