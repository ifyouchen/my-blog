package com.myblog.growth.domain.service;

import com.myblog.growth.domain.model.valueobject.GrowthRule;
import org.springframework.stereotype.Service;

/**
 * 经验发放领域服务.
 * <p>
 * 负责根据规则和每日计数器计算本次实际可发放的经验量，
 * 不含任何 Spring 持久化依赖，便于单元测试。
 * </p>
 */
@Service
public class ExpGrantDomainService {

    /**
     * 计算本次实际可发放的经验量.
     * <p>
     * 规则如下：
     * <ul>
     *   <li>{@code dailyLimit = 0}：无上限，直接返回 {@code rule.expAmount}</li>
     *   <li>{@code SKIP 策略}：已达上限则返回 0，否则返回 {@code rule.expAmount}</li>
     *   <li>{@code PARTIAL 策略}：返回剩余可发量（min(rule.expAmount, limit - grantedToday)），最小为 0</li>
     * </ul>
     * </p>
     *
     * @param rule         经验规则（包含 expAmount、dailyLimit、strategy）
     * @param grantedToday 今日已发经验量
     * @return 本次实际可发放的经验量（≥ 0）
     */
    public int calcGrantAmount(GrowthRule rule, int grantedToday) {
        int dailyLimit = rule.getDailyLimit();

        // 无上限
        if (dailyLimit == 0) {
            return rule.getExpAmount();
        }

        int limitExp = dailyLimit * rule.getExpAmount();

        if ("PARTIAL".equals(rule.getDailyLimitStrategy())) {
            // PARTIAL：计算剩余可发量
            int remaining = limitExp - grantedToday;
            return Math.max(0, Math.min(rule.getExpAmount(), remaining));
        } else {
            // SKIP（默认）：已达上限则跳过
            return grantedToday >= limitExp ? 0 : rule.getExpAmount();
        }
    }
}

