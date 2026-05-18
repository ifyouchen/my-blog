package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.model.valueobject.UserSignInStats;
import com.myblog.growth.domain.repository.ConsecutiveSignInRewardRepository;
import com.myblog.growth.domain.repository.CumulativeSignInRewardRepository;
import com.myblog.growth.domain.repository.PointRuleRepository;
import com.myblog.growth.domain.repository.RewardGrantLogRepository;
import com.myblog.growth.domain.repository.UserSignInStatsRepository;
import com.myblog.growth.domain.service.SignInDomainService;
import com.myblog.growth.infrastructure.repository.persistence.repository.SignInRecordRepositoryImpl;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 签到奖励应用服务.
 *
 * <p>
 * 负责签到业务全流程：幂等校验 → 连续天数计算 → 阶梯奖励 → 积分入账 → 统计更新 → 里程碑检查。
 * 基础积分从 {@code point_rule_config.SIGN_IN} 读取，连续奖励从 {@code sign_in_consecutive_reward_config} 读取。
 * 事务内完成所有写操作，保证原子性。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
@Service
public class SignInAppService {

    private static final Logger log = LoggerFactory.getLogger(SignInAppService.class);

    private static final String SOURCE_SIGN_IN = "SIGN_IN";
    private static final String REWARD_TYPE_CUMULATIVE = "CUMULATIVE_SIGN";

    private final SignInRecordRepositoryImpl signInRecordRepository;
    private final SignInDomainService signInDomainService;
    private final PointAppService pointAppService;
    private final PointRuleRepository pointRuleRepository;
    private final UserSignInStatsRepository userSignInStatsRepository;
    private final ConsecutiveSignInRewardRepository consecutiveRewardRepository;
    private final CumulativeSignInRewardRepository cumulativeRewardRepository;
    private final RewardGrantLogRepository rewardGrantLogRepository;
    private final BadgeAppService badgeAppService;

    public SignInAppService(SignInRecordRepositoryImpl signInRecordRepository,
                            SignInDomainService signInDomainService,
                            PointAppService pointAppService,
                            PointRuleRepository pointRuleRepository,
                            UserSignInStatsRepository userSignInStatsRepository,
                            ConsecutiveSignInRewardRepository consecutiveRewardRepository,
                            CumulativeSignInRewardRepository cumulativeRewardRepository,
                            RewardGrantLogRepository rewardGrantLogRepository,
                            BadgeAppService badgeAppService) {
        this.signInRecordRepository = signInRecordRepository;
        this.signInDomainService = signInDomainService;
        this.pointAppService = pointAppService;
        this.pointRuleRepository = pointRuleRepository;
        this.userSignInStatsRepository = userSignInStatsRepository;
        this.consecutiveRewardRepository = consecutiveRewardRepository;
        this.cumulativeRewardRepository = cumulativeRewardRepository;
        this.rewardGrantLogRepository = rewardGrantLogRepository;
        this.badgeAppService = badgeAppService;
    }

    /**
     * 里程碑奖励信息.
     */
    public static class MilestoneRewardInfo {
        private final int milestoneDays;
        private final int rewardPoints;
        private final String rewardTitle;
        private final String description;

        public MilestoneRewardInfo(int milestoneDays, int rewardPoints, String rewardTitle, String description) {
            this.milestoneDays = milestoneDays;
            this.rewardPoints = rewardPoints;
            this.rewardTitle = rewardTitle;
            this.description = description;
        }

        public int getMilestoneDays() { return milestoneDays; }
        public int getRewardPoints() { return rewardPoints; }
        public String getRewardTitle() { return rewardTitle; }
        public String getDescription() { return description; }
    }

    /**
     * 签到结果值对象.
     */
    public static class SignInResult {
        private final LocalDate signDate;
        private final int consecutiveDays;
        private final int totalSignDays;
        private final int basePoints;
        private final int consecutiveBonus;
        private final int milestoneBonus;
        private final int pointsGranted;
        private final int totalBalance;
        private final String rewardTier;
        private final String rewardDesc;
        private final List<MilestoneRewardInfo> newMilestones;

        public SignInResult(LocalDate signDate, int consecutiveDays, int totalSignDays,
                            int basePoints, int consecutiveBonus, int milestoneBonus,
                            int pointsGranted, int totalBalance, String rewardTier,
                            String rewardDesc, List<MilestoneRewardInfo> newMilestones) {
            this.signDate = signDate;
            this.consecutiveDays = consecutiveDays;
            this.totalSignDays = totalSignDays;
            this.basePoints = basePoints;
            this.consecutiveBonus = consecutiveBonus;
            this.milestoneBonus = milestoneBonus;
            this.pointsGranted = pointsGranted;
            this.totalBalance = totalBalance;
            this.rewardTier = rewardTier;
            this.rewardDesc = rewardDesc;
            this.newMilestones = newMilestones;
        }

        public LocalDate getSignDate() { return signDate; }
        public int getConsecutiveDays() { return consecutiveDays; }
        public int getTotalSignDays() { return totalSignDays; }
        public int getBasePoints() { return basePoints; }
        public int getConsecutiveBonus() { return consecutiveBonus; }
        public int getMilestoneBonus() { return milestoneBonus; }
        public int getPointsGranted() { return pointsGranted; }
        public int getTotalBalance() { return totalBalance; }
        public String getRewardTier() { return rewardTier; }
        public String getRewardDesc() { return rewardDesc; }
        public List<MilestoneRewardInfo> getNewMilestones() { return newMilestones; }
    }

    /**
     * 执行签到.
     *
     * @param userId 用户 ID
     * @return 签到结果
     * @throws GrowthBusinessException 今日已签到时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public SignInResult signIn(Long userId) {
        LocalDate today = LocalDate.now();

        // ① 检查今日是否已签到
        if (signInRecordRepository.existsByUserIdAndDate(userId, today)) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "今日已签到");
        }

        // ② 计算连续天数
        LocalDate lastSignDate = signInRecordRepository.findLastSignDate(userId);
        int consecutiveDays;
        if (lastSignDate == null) {
            consecutiveDays = 1;
        } else if (lastSignDate.isEqual(today.minusDays(1))) {
            int lastConsecutive = signInRecordRepository.findContinuousDays(userId);
            consecutiveDays = lastConsecutive + 1;
        } else {
            consecutiveDays = 1;
        }

        // ③ 从配置表读取连续签到奖励
        List<ConsecutiveSignInRewardConfig> configs = consecutiveRewardRepository.findAllEnabled();
        int consecutiveBonus = signInDomainService.calcBonus(consecutiveDays, configs);
        String tier = signInDomainService.rewardTier(consecutiveDays, configs);
        String desc = signInDomainService.rewardDesc(tier, configs, consecutiveDays);

        // ④ 从 point_rule_config 读取基础签到积分
        int basePoints = 0;
        boolean ruleEnabled = false;
        Optional<PointRule> ruleOpt = pointRuleRepository.findBySourceType(SOURCE_SIGN_IN);
        if (ruleOpt.isPresent() && ruleOpt.get().isEffective()) {
            basePoints = ruleOpt.get().getPointAmount();
            ruleEnabled = true;
        }

        // 计算签到基础积分（不含里程碑）
        int signInPoints = ruleEnabled ? (basePoints + consecutiveBonus) : 0;
        if (!ruleEnabled) {
            tier = "DISABLED";
            desc = "签到奖励已关闭";
        }

        // ⑤ INSERT IGNORE sign_in_record
        int inserted = signInRecordRepository.insertIgnore(userId, today, consecutiveDays, signInPoints);
        if (inserted == 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "今日已签到");
        }

        // ⑥ 更新 user_sign_in_stats
        int totalSignDays = updateUserSignInStats(userId, today, consecutiveDays);

        // ⑦ 检查累计签到里程碑 → 发放奖励
        List<MilestoneRewardInfo> newMilestones = checkAndGrantCumulativeMilestones(userId, totalSignDays);
        int milestoneBonus = newMilestones.stream().mapToInt(MilestoneRewardInfo::getRewardPoints).sum();

        // ⑧ 积分入账
        int totalPointsGranted = signInPoints + milestoneBonus;
        int totalBalance = 0;
        if (totalPointsGranted > 0) {
            String bizNo = "signin-" + userId + "-" + today;
            String remark = "每日签到奖励（连续" + consecutiveDays + "天）"
                    + (milestoneBonus > 0 ? " + 里程碑奖励" : "");
            totalBalance = pointAppService.addPoints(userId, totalPointsGranted, SOURCE_SIGN_IN, bizNo, remark, null);
        } else {
            totalBalance = pointAppService.getAccount(userId).getBalance();
            log.info("[签到] 积分规则未启用，签到积分为 0。userId={}", userId);
        }

        log.info("[签到] 成功。userId={}, today={}, consecutiveDays={}, totalSignDays={}, pointsGranted={}",
                userId, today, consecutiveDays, totalSignDays, totalPointsGranted);

        return new SignInResult(today, consecutiveDays, totalSignDays,
                basePoints, consecutiveBonus, milestoneBonus,
                totalPointsGranted, totalBalance, tier, desc, newMilestones);
    }

    /**
     * 签到统计结果.
     */
    public static class SignInStatsResult {
        private final int totalSignDays;
        private final int currentStreak;
        private final int longestStreak;
        private final LocalDate lastSignDate;
        private final NextMilestoneInfo nextMilestone;

        public SignInStatsResult(int totalSignDays, int currentStreak, int longestStreak,
                                  LocalDate lastSignDate, NextMilestoneInfo nextMilestone) {
            this.totalSignDays = totalSignDays;
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
            this.lastSignDate = lastSignDate;
            this.nextMilestone = nextMilestone;
        }

        public int getTotalSignDays() { return totalSignDays; }
        public int getCurrentStreak() { return currentStreak; }
        public int getLongestStreak() { return longestStreak; }
        public LocalDate getLastSignDate() { return lastSignDate; }
        public NextMilestoneInfo getNextMilestone() { return nextMilestone; }
    }

    /**
     * 下一个里程碑信息.
     */
    public static class NextMilestoneInfo {
        private final int milestoneDays;
        private final int rewardPoints;
        private final String rewardTitle;
        private final int progress;
        private final int remaining;

        public NextMilestoneInfo(int milestoneDays, int rewardPoints, String rewardTitle,
                                  int progress, int remaining) {
            this.milestoneDays = milestoneDays;
            this.rewardPoints = rewardPoints;
            this.rewardTitle = rewardTitle;
            this.progress = progress;
            this.remaining = remaining;
        }

        public int getMilestoneDays() { return milestoneDays; }
        public int getRewardPoints() { return rewardPoints; }
        public String getRewardTitle() { return rewardTitle; }
        public int getProgress() { return progress; }
        public int getRemaining() { return remaining; }
    }

    /**
     * 查询签到统计.
     *
     * @param userId 用户ID
     * @return 签到统计结果
     */
    public SignInStatsResult getSignInStats(Long userId) {
        UserSignInStats stats = userSignInStatsRepository.findByUserId(userId)
                .orElse(UserSignInStats.create(userId));

        List<CumulativeSignInRewardConfig> allMilestones = cumulativeRewardRepository.findAllEnabled();
        NextMilestoneInfo nextMilestone = findNextMilestone(stats.getTotalSignDays(), allMilestones);

        return new SignInStatsResult(
                stats.getTotalSignDays(),
                stats.getCurrentStreak(),
                stats.getLongestStreak(),
                stats.getLastSignDate(),
                nextMilestone
        );
    }

    /**
     * 查询里程碑列表.
     *
     * @param userId 用户ID
     * @return 里程碑列表
     */
    public List<MilestoneRewardInfo> getMilestones(Long userId) {
        List<CumulativeSignInRewardConfig> allMilestones = cumulativeRewardRepository.findAllEnabled();
        int totalSignDays = userSignInStatsRepository.findByUserId(userId)
                .map(UserSignInStats::getTotalSignDays)
                .orElse(0);

        return allMilestones.stream()
                .map(config -> {
                    boolean achieved = config.getMilestoneDays() <= totalSignDays;
                    return new MilestoneRewardInfo(
                            config.getMilestoneDays(),
                            config.getRewardPoints(),
                            config.getRewardTitle(),
                            config.getDescription()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 签到日历查询结果值对象.
     */
    public static class SignInCalendarResult {
        private final String month;
        private final List<LocalDate> signedDates;
        private final int currentConsecutiveDays;
        private final int totalSignDaysThisMonth;
        private final boolean todaySigned;

        public SignInCalendarResult(String month, List<LocalDate> signedDates,
                                    int currentConsecutiveDays, int totalSignDaysThisMonth,
                                    boolean todaySigned) {
            this.month = month;
            this.signedDates = signedDates;
            this.currentConsecutiveDays = currentConsecutiveDays;
            this.totalSignDaysThisMonth = totalSignDaysThisMonth;
            this.todaySigned = todaySigned;
        }

        public String getMonth() { return month; }
        public List<LocalDate> getSignedDates() { return signedDates; }
        public int getCurrentConsecutiveDays() { return currentConsecutiveDays; }
        public int getTotalSignDaysThisMonth() { return totalSignDaysThisMonth; }
        public boolean isTodaySigned() { return todaySigned; }
    }

    /**
     * 查询签到日历.
     */
    public SignInCalendarResult getCalendar(Long userId, String month) {
        YearMonth ym = (month == null || month.trim().isEmpty())
                ? YearMonth.now()
                : YearMonth.parse(month);

        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<LocalDate> signedDates = signInRecordRepository.findSignDatesByMonth(userId, monthStart, monthEnd);
        int currentConsecutiveDays = signInRecordRepository.findContinuousDays(userId);
        boolean todaySigned = signInRecordRepository.existsByUserIdAndDate(userId, today);

        return new SignInCalendarResult(
                ym.toString(),
                signedDates,
                currentConsecutiveDays,
                signedDates.size(),
                todaySigned
        );
    }

    // ────────────────────────── 私有辅助方法 ──────────────────────────────

    private int updateUserSignInStats(Long userId, LocalDate today, int consecutiveDays) {
        UserSignInStats stats = userSignInStatsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSignInStats newStats = UserSignInStats.create(userId);
                    userSignInStatsRepository.save(newStats);
                    return userSignInStatsRepository.findByUserId(userId).orElse(newStats);
                });

        stats.recordSignIn(today, consecutiveDays);
        int updated = userSignInStatsRepository.updateWithCAS(stats);
        if (updated == 0) {
            log.warn("[签到统计] 乐观锁冲突，重试更新。userId={}", userId);
            stats = userSignInStatsRepository.findByUserId(userId).orElse(stats);
            stats.recordSignIn(today, consecutiveDays);
            userSignInStatsRepository.updateWithCAS(stats);
        }

        return stats.getTotalSignDays();
    }

    private List<MilestoneRewardInfo> checkAndGrantCumulativeMilestones(Long userId, int totalSignDays) {
        List<CumulativeSignInRewardConfig> eligibleMilestones =
                cumulativeRewardRepository.findByMilestoneLessThanEqual(totalSignDays);

        List<MilestoneRewardInfo> granted = new ArrayList<>();
        for (CumulativeSignInRewardConfig config : eligibleMilestones) {
            if (rewardGrantLogRepository.existsByUserAndReward(userId, REWARD_TYPE_CUMULATIVE, config.getId())) {
                continue;
            }

            RewardGrantLog grantLog = RewardGrantLog.create(
                    userId, REWARD_TYPE_CUMULATIVE, config.getId(),
                    config.getRewardPoints(), "累计签到 " + config.getMilestoneDays() + " 天里程碑");
            rewardGrantLogRepository.save(grantLog);
            badgeAppService.grantSignInBadge(userId, config.getBadgeCode(), config.getMilestoneDays());

            if (config.getRewardPoints() > 0) {
                String bizNo = "milestone-" + userId + "-" + config.getMilestoneDays();
                pointAppService.addPoints(userId, config.getRewardPoints(), SOURCE_SIGN_IN, bizNo,
                        "累计签到 " + config.getMilestoneDays() + " 天里程碑奖励", null);
            }

            granted.add(new MilestoneRewardInfo(
                    config.getMilestoneDays(),
                    config.getRewardPoints(),
                    config.getRewardTitle(),
                    config.getDescription()
            ));

            log.info("[里程碑] 发放成功。userId={}, milestoneDays={}, points={}",
                    userId, config.getMilestoneDays(), config.getRewardPoints());
        }

        return granted;
    }

    private NextMilestoneInfo findNextMilestone(int totalSignDays, List<CumulativeSignInRewardConfig> allMilestones) {
        for (CumulativeSignInRewardConfig config : allMilestones) {
            if (config.getMilestoneDays() > totalSignDays) {
                return new NextMilestoneInfo(
                        config.getMilestoneDays(),
                        config.getRewardPoints(),
                        config.getRewardTitle(),
                        totalSignDays,
                        config.getMilestoneDays() - totalSignDays
                );
            }
        }
        return null;
    }
}
