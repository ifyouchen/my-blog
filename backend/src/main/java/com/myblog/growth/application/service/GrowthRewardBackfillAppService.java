package com.myblog.growth.application.service;

import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.repository.UserRepository;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.model.valueobject.UserSignInStats;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.LevelRewardRepository;
import com.myblog.growth.domain.repository.LevelPrivilegeRepository;
import com.myblog.growth.domain.repository.PointJournalRepository;
import com.myblog.growth.domain.repository.RewardGrantLogRepository;
import com.myblog.growth.domain.repository.UserSignInStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private static final String REWARD_TYPE_LEVEL_UP = "LEVEL_UP";
    private static final String SOURCE_LEVEL_UP = "LEVEL_UP";

    private final UserRepository userRepository;
    private final GrowthAccountRepository growthAccountRepository;
    private final PointAppService pointAppService;
    private final UserPrivilegeAppService userPrivilegeAppService;
    private final LevelPrivilegeRepository levelPrivilegeRepository;
    private final LevelRewardRepository levelRewardRepository;
    private final RewardGrantLogRepository rewardGrantLogRepository;
    private final PointJournalRepository pointJournalRepository;
    private final UserSignInStatsRepository userSignInStatsRepository;
    private final BadgeAppService badgeAppService;

    public GrowthRewardBackfillAppService(UserRepository userRepository,
                                          GrowthAccountRepository growthAccountRepository,
                                          PointAppService pointAppService,
                                          UserPrivilegeAppService userPrivilegeAppService,
                                          LevelPrivilegeRepository levelPrivilegeRepository,
                                          LevelRewardRepository levelRewardRepository,
                                          RewardGrantLogRepository rewardGrantLogRepository,
                                          PointJournalRepository pointJournalRepository,
                                          UserSignInStatsRepository userSignInStatsRepository,
                                          BadgeAppService badgeAppService) {
        this.userRepository = userRepository;
        this.growthAccountRepository = growthAccountRepository;
        this.pointAppService = pointAppService;
        this.userPrivilegeAppService = userPrivilegeAppService;
        this.levelPrivilegeRepository = levelPrivilegeRepository;
        this.levelRewardRepository = levelRewardRepository;
        this.rewardGrantLogRepository = rewardGrantLogRepository;
        this.pointJournalRepository = pointJournalRepository;
        this.userSignInStatsRepository = userSignInStatsRepository;
        this.badgeAppService = badgeAppService;
    }

    /**
     * 补偿全部用户.
     *
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> backfillAll() {
        int registerBonusFixed = 0;
        int levelRewardFixed = 0;
        int privilegeFixed = 0;
        int badgeFixed = 0;
        List<LevelPrivilegeConfig> privilegeConfigs = levelPrivilegeRepository.findAllEnabled();
        List<LevelRewardConfig> levelRewardConfigs = levelRewardRepository.findAllEnabled();
        List<User> users = userRepository.findAll();
        List<Long> userIds = new ArrayList<Long>();
        List<String> registerBizNos = new ArrayList<String>();
        for (User user : users) {
            Long userId = user.getId().getValue();
            userIds.add(userId);
            registerBizNos.add("REGISTER:" + userId);
        }
        Map<Long, GrowthAccount> accountMap = growthAccountRepository.findByUserIds(userIds);
        Map<Long, UserSignInStats> signStatsMap = userSignInStatsRepository.findByUserIds(userIds);
        Map<Long, List<String>> privilegeCodesMap = userPrivilegeAppService.listPrivilegeCodesByUserIds(userIds);
        Set<String> existingRegisterBizNos = pointJournalRepository.findExistingBizNos(registerBizNos);
        for (Long userId : userIds) {
            GrowthAccount account = accountMap.get(userId);
            UserSignInStats signStats = signStatsMap.get(userId);
            int currentLevel = account == null ? 1 : account.getCurrentLevel();
            int totalSignDays = signStats == null ? 0 : signStats.getTotalSignDays();
            Map<String, Integer> result = backfillUserInternal(
                userId,
                levelRewardConfigs,
                privilegeConfigs,
                existingRegisterBizNos.contains("REGISTER:" + userId),
                currentLevel,
                totalSignDays,
                privilegeCodesMap.get(userId));
            registerBonusFixed += result.get("registerBonusFixed");
            levelRewardFixed += result.get("levelRewardFixed");
            privilegeFixed += result.get("privilegeFixed");
            badgeFixed += result.get("badgeFixed");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("mode", "ALL");
        result.put("registerBonusFixed", registerBonusFixed);
        result.put("levelRewardFixed", levelRewardFixed);
        result.put("privilegeFixed", privilegeFixed);
        result.put("badgeFixed", badgeFixed);
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
        Map<String, Integer> counters = backfillUserInternal(
            userId,
            levelRewardRepository.findAllEnabled(),
            levelPrivilegeRepository.findAllEnabled());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("mode", "USER");
        result.put("userId", userId);
        result.putAll(counters);
        return result;
    }

    private Map<String, Integer> backfillUserInternal(Long userId,
                                                      List<LevelRewardConfig> levelRewardConfigs,
                                                      List<LevelPrivilegeConfig> privilegeConfigs) {
        int currentLevel = growthAccountRepository.findByUserId(userId)
            .map(GrowthAccount::getCurrentLevel)
            .orElse(1);
        int totalSignDays = userSignInStatsRepository.findByUserId(userId)
            .map(UserSignInStats::getTotalSignDays)
            .orElse(0);
        return backfillUserInternal(
            userId,
            levelRewardConfigs,
            privilegeConfigs,
            hasRegisterBonus(userId),
            currentLevel,
            totalSignDays,
            userPrivilegeAppService.listPrivilegeCodes(userId));
    }

    private Map<String, Integer> backfillUserInternal(Long userId,
                                                      List<LevelRewardConfig> levelRewardConfigs,
                                                      List<LevelPrivilegeConfig> privilegeConfigs,
                                                      boolean hasRegisterBonus,
                                                      int currentLevel,
                                                      int totalSignDays,
                                                      List<String> privilegeCodes) {
        int registerBonusFixed = 0;
        int levelRewardFixed = 0;
        int privilegeFixed = 0;
        int badgeFixed = 0;
        if (!hasRegisterBonus) {
            pointAppService.addPoints(userId, 10, "REGISTER_BONUS", "REGISTER:" + userId, "注册奖励", "SYSTEM");
            registerBonusFixed = 1;
        }
        levelRewardFixed = backfillLevelRewards(userId, currentLevel, levelRewardConfigs);
        Set<Integer> processedLevels = new LinkedHashSet<Integer>();
        for (LevelPrivilegeConfig privilegeConfig : privilegeConfigs) {
            if (privilegeConfig.getLevel() <= currentLevel && processedLevels.add(privilegeConfig.getLevel())) {
                privilegeFixed += userPrivilegeAppService.grantLevelPrivileges(userId, privilegeConfig.getLevel()).size();
            }
        }
        List<String> latestPrivilegeCodes = privilegeFixed > 0
            ? userPrivilegeAppService.listPrivilegeCodes(userId)
            : privilegeCodes;
        badgeFixed = badgeAppService.backfillBadges(userId, currentLevel, totalSignDays, latestPrivilegeCodes);
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        result.put("registerBonusFixed", registerBonusFixed);
        result.put("levelRewardFixed", levelRewardFixed);
        result.put("privilegeFixed", privilegeFixed);
        result.put("badgeFixed", badgeFixed);
        return result;
    }

    private int backfillLevelRewards(Long userId, int currentLevel, List<LevelRewardConfig> levelRewardConfigs) {
        int fixed = 0;
        if (levelRewardConfigs == null || levelRewardConfigs.isEmpty()) {
            return fixed;
        }
        for (LevelRewardConfig config : levelRewardConfigs) {
            if (config.getLevel() > currentLevel || config.getRewardPoints() <= 0) {
                continue;
            }
            String bizNo = buildLevelRewardBizNo(userId, config.getLevel());
            boolean pointJournalExists = pointJournalRepository.findByBizNo(bizNo).isPresent();
            if (!pointJournalExists) {
                pointAppService.addPoints(
                    userId,
                    config.getRewardPoints(),
                    SOURCE_LEVEL_UP,
                    bizNo,
                    "升级到 " + config.getLevel() + " 级（" + config.getRewardTitle() + "）奖励",
                    null);
                fixed++;
            }
            if (!rewardGrantLogRepository.existsByUserAndReward(userId, REWARD_TYPE_LEVEL_UP, config.getId())) {
                RewardGrantLog grantLog = RewardGrantLog.create(
                    userId,
                    REWARD_TYPE_LEVEL_UP,
                    config.getId(),
                    config.getRewardPoints(),
                    "升级到 " + config.getLevel() + " 级奖励");
                rewardGrantLogRepository.save(grantLog);
                fixed++;
            }
        }
        return fixed;
    }

    private String buildLevelRewardBizNo(Long userId, int level) {
        return "levelup-" + userId + "-" + level;
    }

    private boolean hasRegisterBonus(Long userId) {
        return pointJournalRepository.findByBizNo("REGISTER:" + userId).isPresent();
    }
}
