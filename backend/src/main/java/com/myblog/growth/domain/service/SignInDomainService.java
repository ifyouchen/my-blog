package com.myblog.growth.domain.service;

import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 签到领域服务.
 * <p>
 * 负责计算连续签到奖励积分（从配置表读取，带硬编码 fallback）。
 * 基础积分从 {@code point_rule_config.SIGN_IN} 读取（由应用服务负责），
 * 本服务只计算连续签到奖励部分。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
@Service
public class SignInDomainService {

    /**
     * 根据连续签到天数计算附加奖励（从配置表读取）.
     *
     * @param consecutiveDays 连续签到天数（含今日，必须 &gt;= 1）
     * @param configs         连续签到奖励配置列表
     * @return 附加奖励积分
     */
    public int calcBonus(int consecutiveDays, List<ConsecutiveSignInRewardConfig> configs) {
        if (configs != null && !configs.isEmpty()) {
            for (ConsecutiveSignInRewardConfig config : configs) {
                if (config.matches(consecutiveDays)) {
                    return config.getBonusPoints();
                }
            }
        }
        return calcBonusFallback(consecutiveDays);
    }

    /**
     * 根据连续天数返回奖励档位标识（从配置表读取）.
     *
     * @param consecutiveDays 连续签到天数
     * @param configs         连续签到奖励配置列表
     * @return 奖励档位标识
     */
    public String rewardTier(int consecutiveDays, List<ConsecutiveSignInRewardConfig> configs) {
        if (configs != null && !configs.isEmpty()) {
            for (ConsecutiveSignInRewardConfig config : configs) {
                if (config.matches(consecutiveDays)) {
                    return config.getRewardTier();
                }
            }
        }
        return rewardTierFallback(consecutiveDays);
    }

    /**
     * 根据档位返回奖励描述（从配置表读取）.
     *
     * @param tier    档位标识
     * @param configs 连续签到奖励配置列表
     * @return 奖励描述文案
     */
    public String rewardDesc(String tier, List<ConsecutiveSignInRewardConfig> configs, int consecutiveDays) {
        if (configs != null && !configs.isEmpty()) {
            for (ConsecutiveSignInRewardConfig config : configs) {
                if (config.getRewardTier().equals(tier) && config.matches(consecutiveDays)) {
                    return config.getRewardDesc();
                }
            }
        }
        return rewardDescFallback(tier);
    }

    // ────────────────────────── 硬编码 Fallback ──────────────────────────────

    private int calcBonusFallback(int consecutiveDays) {
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

    private String rewardTierFallback(int consecutiveDays) {
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

    private String rewardDescFallback(String tier) {
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
