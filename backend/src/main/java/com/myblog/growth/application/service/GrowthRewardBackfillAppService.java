package com.myblog.growth.application.service;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.repository.UserRepository;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.LevelPrivilegeRepository;
import com.myblog.growth.domain.repository.PointJournalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 成长奖励补偿应用服务.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Service
public class GrowthRewardBackfillAppService {

    private final UserRepository userRepository;
    private final GrowthAccountRepository growthAccountRepository;
    private final PointAppService pointAppService;
    private final UserPrivilegeAppService userPrivilegeAppService;
    private final LevelPrivilegeRepository levelPrivilegeRepository;
    private final PointJournalRepository pointJournalRepository;

    public GrowthRewardBackfillAppService(UserRepository userRepository,
                                          GrowthAccountRepository growthAccountRepository,
                                          PointAppService pointAppService,
                                          UserPrivilegeAppService userPrivilegeAppService,
                                          LevelPrivilegeRepository levelPrivilegeRepository,
                                          PointJournalRepository pointJournalRepository) {
        this.userRepository = userRepository;
        this.growthAccountRepository = growthAccountRepository;
        this.pointAppService = pointAppService;
        this.userPrivilegeAppService = userPrivilegeAppService;
        this.levelPrivilegeRepository = levelPrivilegeRepository;
        this.pointJournalRepository = pointJournalRepository;
    }

    /**
     * 补偿全部用户.
     *
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> backfillAll() {
        int registerBonusFixed = 0;
        int privilegeFixed = 0;
        List<LevelPrivilegeConfig> privilegeConfigs = levelPrivilegeRepository.findAllEnabled();
        for (User user : userRepository.findAll()) {
            Map<String, Integer> result = backfillUserInternal(user.getId().getValue(), privilegeConfigs);
            registerBonusFixed += result.get("registerBonusFixed");
            privilegeFixed += result.get("privilegeFixed");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("mode", "ALL");
        result.put("registerBonusFixed", registerBonusFixed);
        result.put("privilegeFixed", privilegeFixed);
        return result;
    }

    /**
     * 补偿单个用户.
     *
     * @param userId 用户ID
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> backfillUser(Long userId) {
        Map<String, Integer> counters = backfillUserInternal(userId, levelPrivilegeRepository.findAllEnabled());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("mode", "USER");
        result.put("userId", userId);
        result.putAll(counters);
        return result;
    }

    private Map<String, Integer> backfillUserInternal(Long userId, List<LevelPrivilegeConfig> privilegeConfigs) {
        int registerBonusFixed = 0;
        int privilegeFixed = 0;
        if (!hasRegisterBonus(userId)) {
            pointAppService.addPoints(userId, 10, "REGISTER_BONUS", "REGISTER:" + userId, "注册奖励", "SYSTEM");
            registerBonusFixed = 1;
        }
        int currentLevel = growthAccountRepository.findByUserId(userId)
            .map(GrowthAccount::getCurrentLevel)
            .orElse(1);
        Set<Integer> processedLevels = new LinkedHashSet<Integer>();
        for (LevelPrivilegeConfig privilegeConfig : privilegeConfigs) {
            if (privilegeConfig.getLevel() <= currentLevel && processedLevels.add(privilegeConfig.getLevel())) {
                privilegeFixed += userPrivilegeAppService.grantLevelPrivileges(userId, privilegeConfig.getLevel()).size();
            }
        }
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        result.put("registerBonusFixed", registerBonusFixed);
        result.put("privilegeFixed", privilegeFixed);
        return result;
    }

    private boolean hasRegisterBonus(Long userId) {
        return pointJournalRepository.findByBizNo("REGISTER:" + userId).isPresent();
    }
}
