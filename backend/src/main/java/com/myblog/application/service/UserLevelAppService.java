package com.myblog.application.service;

import com.myblog.application.dto.UserDTO;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.UserPrivilegeEntitlementRepository;
import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import com.myblog.growth.application.service.BadgeAppService;
import com.myblog.growth.shared.constant.GrowthPrivilegeCodes;
import com.myblog.shared.dto.BadgeDisplayDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户等级展示填充服务。
 */
@Service
public class UserLevelAppService {

    private final GrowthAccountRepository growthAccountRepository;
    private final UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository;
    private final BadgeAppService badgeAppService;

    public UserLevelAppService(GrowthAccountRepository growthAccountRepository,
                               UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository,
                               BadgeAppService badgeAppService) {
        this.growthAccountRepository = growthAccountRepository;
        this.userPrivilegeEntitlementRepository = userPrivilegeEntitlementRepository;
        this.badgeAppService = badgeAppService;
    }

    /**
     * 为单个用户填充当前等级，缺失成长账户时按 LV1 展示。
     *
     * @param user 用户 DTO
     */
    public void fillLevel(UserDTO user) {
        if (user == null || user.getId() == null) {
            return;
        }
        int level = growthAccountRepository.findByUserId(user.getId())
            .map(GrowthAccount::getCurrentLevel)
            .orElse(1);
        user.setCurrentLevel(Math.max(1, level));
        List<String> privilegeCodes = new ArrayList<String>();
        for (UserPrivilegeEntitlement entitlement : userPrivilegeEntitlementRepository.findActiveByUserId(user.getId())) {
            privilegeCodes.add(entitlement.getPrivilegeCode());
        }
        user.setPrivilegeCodes(privilegeCodes);
        user.setExclusiveBadgeEnabled(privilegeCodes.contains(GrowthPrivilegeCodes.EXCLUSIVE_BADGE));
        Map<Long, BadgeDisplayDTO> badgeMap = badgeAppService.findEquippedBadges(java.util.Collections.singletonList(user.getId()));
        user.setEquippedBadge(badgeMap.get(user.getId()));
    }

    /**
     * 为多个用户批量填充当前等级，缺失成长账户时按 LV1 展示。
     *
     * @param users 用户 DTO 集合
     */
    public void fillLevels(Collection<UserDTO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> userIds = new LinkedHashSet<Long>();
        List<UserDTO> validUsers = new ArrayList<UserDTO>();
        for (UserDTO user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            validUsers.add(user);
            userIds.add(user.getId());
            if (user.getCurrentLevel() <= 0) {
                user.setCurrentLevel(1);
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, GrowthAccount> accountMap =
            growthAccountRepository.findByUserIds(new ArrayList<Long>(userIds));
        Map<Long, List<UserPrivilegeEntitlement>> privilegeMap =
            userPrivilegeEntitlementRepository.findActiveByUserIds(new ArrayList<Long>(userIds));
        Map<Long, BadgeDisplayDTO> equippedBadgeMap =
            badgeAppService.findEquippedBadges(new ArrayList<Long>(userIds));
        for (UserDTO user : validUsers) {
            GrowthAccount account = accountMap.get(user.getId());
            user.setCurrentLevel(account == null ? 1 : Math.max(1, account.getCurrentLevel()));
            List<UserPrivilegeEntitlement> entitlements = privilegeMap.get(user.getId());
            List<String> codes = new ArrayList<String>();
            if (entitlements != null) {
                for (UserPrivilegeEntitlement entitlement : entitlements) {
                    codes.add(entitlement.getPrivilegeCode());
                }
            }
            user.setPrivilegeCodes(codes);
            user.setExclusiveBadgeEnabled(codes.contains(GrowthPrivilegeCodes.EXCLUSIVE_BADGE));
            user.setEquippedBadge(equippedBadgeMap.get(user.getId()));
        }
    }
}
