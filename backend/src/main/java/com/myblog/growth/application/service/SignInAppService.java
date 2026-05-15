package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.repository.PointRuleRepository;
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
import java.util.List;
import java.util.Optional;

/**
 * 签到奖励应用服务.
 *
 * <p>
 * 负责签到业务全流程：幂等校验 → 连续天数计算 → 阶梯奖励 → 积分入账。
 * 基础积分从 {@code point_rule_config.SIGN_IN} 读取，规则禁用时仍可签到但积分为 0。
 * 事务内完成 INSERT sign_in_record + PointAppService.addPoints，保证原子性。
 * </p>
 */
@Service
public class SignInAppService {

    private static final Logger log = LoggerFactory.getLogger(SignInAppService.class);

    private static final String SOURCE_SIGN_IN = "SIGN_IN";

    private final SignInRecordRepositoryImpl signInRecordRepository;
    private final SignInDomainService signInDomainService;
    private final PointAppService pointAppService;
    private final PointRuleRepository pointRuleRepository;

    public SignInAppService(SignInRecordRepositoryImpl signInRecordRepository,
                            SignInDomainService signInDomainService,
                            PointAppService pointAppService,
                            PointRuleRepository pointRuleRepository) {
        this.signInRecordRepository = signInRecordRepository;
        this.signInDomainService = signInDomainService;
        this.pointAppService = pointAppService;
        this.pointRuleRepository = pointRuleRepository;
    }

    /**
     * 签到结果值对象.
     */
    public static class SignInResult {
        private final LocalDate signDate;
        private final int consecutiveDays;
        private final int pointsGranted;
        private final int totalBalance;
        private final String rewardTier;
        private final String rewardDesc;

        public SignInResult(LocalDate signDate, int consecutiveDays, int pointsGranted,
                            int totalBalance, String rewardTier, String rewardDesc) {
            this.signDate = signDate;
            this.consecutiveDays = consecutiveDays;
            this.pointsGranted = pointsGranted;
            this.totalBalance = totalBalance;
            this.rewardTier = rewardTier;
            this.rewardDesc = rewardDesc;
        }

        public LocalDate getSignDate() { return signDate; }
        public int getConsecutiveDays() { return consecutiveDays; }
        public int getPointsGranted() { return pointsGranted; }
        public int getTotalBalance() { return totalBalance; }
        public String getRewardTier() { return rewardTier; }
        public String getRewardDesc() { return rewardDesc; }
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

        // ③ 从 point_rule_config 读取基础签到积分
        int basePoints = 0;
        boolean ruleEnabled = false;
        Optional<PointRule> ruleOpt = pointRuleRepository.findBySourceType(SOURCE_SIGN_IN);
        if (ruleOpt.isPresent() && ruleOpt.get().isEffective()) {
            basePoints = ruleOpt.get().getPointAmount();
            ruleEnabled = true;
        }

        // 计算实际奖励：基础积分 + 连续签到奖励
        int pointsGranted;
        String tier;
        String desc;
        if (ruleEnabled) {
            // 使用配置的基础积分 + 连续签到奖励
            int bonus = signInDomainService.calcBonus(consecutiveDays);
            pointsGranted = basePoints + bonus;
            tier = signInDomainService.rewardTier(consecutiveDays);
            desc = signInDomainService.rewardDesc(tier);
        } else {
            // 规则禁用，积分为 0
            pointsGranted = 0;
            tier = "DISABLED";
            desc = "签到奖励已关闭";
        }

        // ④ INSERT IGNORE sign_in_record
        int inserted = signInRecordRepository.insertIgnore(userId, today, consecutiveDays, pointsGranted);
        if (inserted == 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "今日已签到");
        }

        // ⑤ 积分入账（pointsGranted 为 0 时仍然记录，但无积分变动）
        int totalBalance = 0;
        if (pointsGranted > 0) {
            String bizNo = "signin-" + userId + "-" + today;
            totalBalance = pointAppService.addPoints(userId, pointsGranted, SOURCE_SIGN_IN, bizNo,
                    "每日签到奖励（连续" + consecutiveDays + "天）", null);
        } else {
            totalBalance = pointAppService.getAccount(userId).getBalance();
            log.info("[签到] 积分规则未启用，签到积分为 0。userId={}", userId);
        }

        log.info("[签到] 成功。userId={}, today={}, consecutiveDays={}, pointsGranted={}",
                userId, today, consecutiveDays, pointsGranted);

        return new SignInResult(today, consecutiveDays, pointsGranted, totalBalance, tier, desc);
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
}
