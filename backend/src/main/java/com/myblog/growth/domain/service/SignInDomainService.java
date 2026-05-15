package com.myblog.growth.domain.service;

import org.springframework.stereotype.Service;

/**
 * 签到领域服务.
 * <p>
 * 负责计算连续签到奖励积分（阶梯奖励），不依赖任何 Repository，纯业务逻辑。
 * 基础积分从 {@code point_rule_config.SIGN_IN} 读取（由应用服务负责），
 * 本服务只计算连续签到奖励部分。
 * </p>
 *
 * <p>连续签到奖励规则：</p>
 * <pre>
 * 连续天数     奖励
 * 1~2 天       0
 * 3~4 天       5
 * 5~6 天       8
 * 7~13 天     10
 * 14~29 天    20
 * 30+ 天      50
 * </pre>
 */
@Service
public class SignInDomainService {

    /**
     * 根据连续签到天数计算附加奖励（不含基础积分）.
     *
     * @param consecutiveDays 连续签到天数（含今日，必须 &gt;= 1）
     * @return 附加奖励积分
     */
    public int calcBonus(int consecutiveDays) {
        if (consecutiveDays >= 30) {
            return 50;
        } else if (consecutiveDays >= 14) {
            return 20;
        } else if (consecutiveDays >= 7) {
            return 10;
        } else if (consecutiveDays >= 5) {
            return 8;
        } else if (consecutiveDays >= 3) {
            return 5;
        } else {
            return 0;
        }
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
