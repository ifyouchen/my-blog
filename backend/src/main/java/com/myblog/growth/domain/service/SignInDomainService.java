package com.myblog.growth.domain.service;

import org.springframework.stereotype.Service;

/**
 * 签到领域服务.
 * <p>
 * 负责计算连续签到奖励积分（阶梯奖励），不依赖任何 Repository，纯业务逻辑。
 * </p>
 *
 * <p>阶梯奖励规则（可配置化，当前版本为常量）：</p>
 * <pre>
 * 连续天数      基础积分  附加奖励  总计
 * 1~2 天           10       0     10
 * 3~4 天           10       5     15
 * 5~6 天           10       8     18
 * 7~13 天          10      10     20
 * 14~29 天         10      20     30
 * 30+ 天           10      50     60
 * </pre>
 */
@Service
public class SignInDomainService {

    /** 基础签到积分. */
    private static final int BASE_POINTS = 10;

    /**
     * 根据连续签到天数计算本次应发积分.
     *
     * @param consecutiveDays 连续签到天数（含今日，必须 &gt;= 1）
     * @return 本次应发积分总量
     */
    public int calcReward(int consecutiveDays) {
        int bonus;
        if (consecutiveDays >= 30) {
            bonus = 50;
        } else if (consecutiveDays >= 14) {
            bonus = 20;
        } else if (consecutiveDays >= 7) {
            bonus = 10;
        } else if (consecutiveDays >= 5) {
            bonus = 8;
        } else if (consecutiveDays >= 3) {
            bonus = 5;
        } else {
            bonus = 0;
        }
        return BASE_POINTS + bonus;
    }

    /**
     * 根据连续天数返回奖励档位标识.
     *
     * @param consecutiveDays 连续签到天数
     * @return 奖励档位标识（NORMAL / TRIPLE / WEEK / BIWEEK / MONTH）
     */
    public String rewardTier(int consecutiveDays) {
        if (consecutiveDays >= 30) {
            return "MONTH";
        } else if (consecutiveDays >= 14) {
            return "BIWEEK";
        } else if (consecutiveDays >= 7) {
            return "WEEK";
        } else if (consecutiveDays >= 3) {
            return "TRIPLE";
        } else {
            return "NORMAL";
        }
    }

    /**
     * 根据档位返回奖励描述.
     *
     * @param tier 档位标识
     * @return 奖励描述文案
     */
    public String rewardDesc(String tier) {
        switch (tier) {
            case "MONTH":
                return "连续签到 30 天，获得丰厚奖励！";
            case "BIWEEK":
                return "连续签到 14 天，获得额外奖励！";
            case "WEEK":
                return "连续签到 7 天，获得额外奖励！";
            case "TRIPLE":
                return "连续签到 3 天，获得小额奖励！";
            default:
                return "签到成功，明天继续！";
        }
    }
}

