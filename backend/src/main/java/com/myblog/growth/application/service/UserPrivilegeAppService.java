package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import com.myblog.growth.domain.repository.LevelPrivilegeRepository;
import com.myblog.growth.domain.repository.UserPrivilegeEntitlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户等级权益应用服务.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Service
public class UserPrivilegeAppService {

    private final LevelPrivilegeRepository levelPrivilegeRepository;
    private final UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository;
    private final BadgeAppService badgeAppService;

    public UserPrivilegeAppService(LevelPrivilegeRepository levelPrivilegeRepository,
                                   UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository,
                                   BadgeAppService badgeAppService) {
        this.levelPrivilegeRepository = levelPrivilegeRepository;
        this.userPrivilegeEntitlementRepository = userPrivilegeEntitlementRepository;
        this.badgeAppService = badgeAppService;
    }

    /**
     * 按等级为用户授予权益.
     *
     * @param userId 用户ID
     * @param level  等级
     * @return 本次成功写入的权益编码
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> grantLevelPrivileges(Long userId, int level) {
        List<String> grantedCodes = new ArrayList<String>();
        for (LevelPrivilegeConfig config : levelPrivilegeRepository.findEnabledByLevel(level)) {
            UserPrivilegeEntitlement entitlement =
                UserPrivilegeEntitlement.grant(userId, config.getPrivilegeCode(), level);
            int inserted = userPrivilegeEntitlementRepository.insertIgnore(entitlement);
            if (inserted > 0) {
                grantedCodes.add(config.getPrivilegeCode());
                if (com.myblog.growth.shared.constant.GrowthPrivilegeCodes.ANNUAL_CREATOR_ELIGIBLE
                        .equals(config.getPrivilegeCode())) {
                    badgeAppService.grantAnnualCreatorCandidateBadge(userId);
                }
            }
        }
        return grantedCodes;
    }

    /**
     * 查询用户当前有效权益编码.
     *
     * @param userId 用户ID
     * @return 权益编码列表
     */
    public List<String> listPrivilegeCodes(Long userId) {
        Set<String> codes = new LinkedHashSet<String>();
        for (UserPrivilegeEntitlement entitlement : userPrivilegeEntitlementRepository.findActiveByUserId(userId)) {
            codes.add(entitlement.getPrivilegeCode());
        }
        return new ArrayList<String>(codes);
    }

    /**
     * 批量查询用户当前有效权益编码.
     *
     * @param userIds 用户ID列表
     * @return 用户ID到权益编码列表的映射
     */
    public Map<Long, List<String>> listPrivilegeCodesByUserIds(List<Long> userIds) {
        Map<Long, List<String>> result = new HashMap<Long, List<String>>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        Map<Long, List<UserPrivilegeEntitlement>> entitlementMap =
            userPrivilegeEntitlementRepository.findActiveByUserIds(userIds);
        for (Map.Entry<Long, List<UserPrivilegeEntitlement>> entry : entitlementMap.entrySet()) {
            Set<String> codes = new LinkedHashSet<String>();
            for (UserPrivilegeEntitlement entitlement : entry.getValue()) {
                codes.add(entitlement.getPrivilegeCode());
            }
            result.put(entry.getKey(), new ArrayList<String>(codes));
        }
        return result;
    }

    /**
     * 判断用户是否拥有指定权益.
     *
     * @param userId        用户ID
     * @param privilegeCode 权益编码
     * @return true 表示拥有
     */
    public boolean hasPrivilege(Long userId, String privilegeCode) {
        return userId != null
            && privilegeCode != null
            && userPrivilegeEntitlementRepository.existsActiveByUserIdAndPrivilegeCode(userId, privilegeCode);
    }
}
