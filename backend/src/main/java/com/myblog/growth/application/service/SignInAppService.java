package com.myblog.growth.application.service;

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

/**
 * 签到奖励应用服务.
 *
 * <p>
 * 负责签到业务全流程：幂等校验 → 连续天数计算 → 阶梯奖励 → 积分入账。
 * 事务内完成 INSERT sign_in_record + PointAppService.addPoints，保证原子性。
 * </p>
 *
 * <pre>
 * 核心流程 signIn()：
 *   ① 检查今日是否已签到 → 已签返回 400
 *   ② 查最近一条签到记录 → 计算连续天数
 *   ③ SignInDomainService.calcReward(consecutiveDays) → pointsGranted
 *   ④ INSERT IGNORE sign_in_record
 *   ⑤ PointAppService.addPoints(SIGN_IN, bizNo = "signin-{userId}-{signDate}")
 * </pre>
 */
@Service
public class SignInAppService {

    private static final Logger log = LoggerFactory.getLogger(SignInAppService.class);

    private final SignInRecordRepositoryImpl signInRecordRepository;
    private final SignInDomainService signInDomainService;
    private final PointAppService pointAppService;

    /**
     * 构造注入.
     *
     * @param signInRecordRepository 签到记录 Repository 实现
     * @param signInDomainService    签到领域服务
     * @param pointAppService        积分应用服务
     */
    public SignInAppService(SignInRecordRepositoryImpl signInRecordRepository,
                            SignInDomainService signInDomainService,
                            PointAppService pointAppService) {
        this.signInRecordRepository = signInRecordRepository;
        this.signInDomainService = signInDomainService;
        this.pointAppService = pointAppService;
    }

    /**
     * 签到结果值对象（用于 Controller 响应组装）.
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
            // 昨日签到 → 连续
            int lastConsecutive = signInRecordRepository.findContinuousDays(userId);
            consecutiveDays = lastConsecutive + 1;
        } else {
            // 断签 → 重置
            consecutiveDays = 1;
        }

        // ③ 计算奖励积分
        int pointsGranted = signInDomainService.calcReward(consecutiveDays);
        String tier = signInDomainService.rewardTier(consecutiveDays);
        String desc = signInDomainService.rewardDesc(tier);

        // ④ INSERT IGNORE sign_in_record
        int inserted = signInRecordRepository.insertIgnore(userId, today, consecutiveDays, pointsGranted);
        if (inserted == 0) {
            // 并发场景下已被其他线程插入（唯一索引冲突）
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "今日已签到");
        }

        // ⑤ 积分入账
        String bizNo = "signin-" + userId + "-" + today;
        int totalBalance = pointAppService.addPoints(userId, pointsGranted, "SIGN_IN", bizNo,
                "每日签到奖励（连续" + consecutiveDays + "天）", null);

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
     *
     * @param userId 用户 ID
     * @param month  月份字符串（格式 yyyy-MM，null 表示当月）
     * @return 签到日历结果
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

