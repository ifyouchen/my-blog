package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 经验规则值对象.
 * <p>
 * 对应数据库 {@code growth_rule_config} 表，描述某一行为类型在特定角色下
 * 每次可获得的经验量以及每日上限策略。值对象不可变，通过工厂方法构建。
 * </p>
 */
public class GrowthRule {

    /** 数据库主键 ID. */
    private Long id;

    /** 行为类型（对应 GrowthEventType，如 LIKE / READ / PUBLISH）. */
    private String eventType;

    /** 作用角色：ACTOR（操作者）/ AUTHOR（被操作内容作者）. */
    private String role;

    /** 单次发放经验量. */
    private int expAmount;

    /** 每日上限次数（0 = 无上限）. */
    private int dailyLimit;

    /** 超限策略：SKIP（跳过）/ PARTIAL（按剩余量发放）. */
    private String dailyLimitStrategy;

    /** 是否启用. */
    private boolean enabled;

    /** 生效时间（null = 立即生效）. */
    private LocalDateTime effectiveAt;

    /** 最后操作人. */
    private String operator;

    /** 变更原因. */
    private String reason;

    /** 乐观锁版本号. */
    private int version;

    /** 禁止外部直接构造，使用工厂方法. */
    private GrowthRule() {
    }

    /**
     * 工厂方法：构建经验规则值对象.
     *
     * @param id                  主键 ID
     * @param eventType           行为类型
     * @param role                作用角色
     * @param expAmount           单次经验量
     * @param dailyLimit          每日上限次数
     * @param dailyLimitStrategy  超限策略
     * @param enabled             是否启用
     * @param effectiveAt         生效时间（null = 立即）
     * @param operator            最后操作人
     * @param reason              变更原因
     * @param version             乐观锁版本号
     * @return 经验规则值对象
     */
    public static GrowthRule of(Long id, String eventType, String role, int expAmount,
                                int dailyLimit, String dailyLimitStrategy, boolean enabled,
                                LocalDateTime effectiveAt, String operator, String reason, int version) {
        GrowthRule rule = new GrowthRule();
        rule.id = id;
        rule.eventType = eventType;
        rule.role = role;
        rule.expAmount = expAmount;
        rule.dailyLimit = dailyLimit;
        rule.dailyLimitStrategy = dailyLimitStrategy;
        rule.enabled = enabled;
        rule.effectiveAt = effectiveAt;
        rule.operator = operator;
        rule.reason = reason;
        rule.version = version;
        return rule;
    }

    /**
     * 判断规则当前是否已生效.
     * <p>规则必须 enabled=true，且 effectiveAt 为 null 或已到期。</p>
     *
     * @return {@code true} 表示已生效，可用于经验发放
     */
    public boolean isEffective() {
        if (!enabled) {
            return false;
        }
        if (effectiveAt == null) {
            return true;
        }
        return !effectiveAt.isAfter(LocalDateTime.now());
    }

    /**
     * 获取主键 ID.
     *
     * @return 主键 ID
     */
    public Long getId() { return id; }

    /**
     * 获取行为类型.
     *
     * @return 行为类型字符串
     */
    public String getEventType() { return eventType; }

    /**
     * 获取作用角色.
     *
     * @return ACTOR 或 AUTHOR
     */
    public String getRole() { return role; }

    /**
     * 获取单次发放经验量.
     *
     * @return 单次经验量（> 0）
     */
    public int getExpAmount() { return expAmount; }

    /**
     * 获取每日上限次数.
     *
     * @return 每日上限次数，0 表示无上限
     */
    public int getDailyLimit() { return dailyLimit; }

    /**
     * 获取超限策略.
     *
     * @return SKIP 或 PARTIAL
     */
    public String getDailyLimitStrategy() { return dailyLimitStrategy; }

    /**
     * 获取是否启用.
     *
     * @return true 表示启用
     */
    public boolean isEnabled() { return enabled; }

    /**
     * 获取生效时间.
     *
     * @return 生效时间，null 表示立即生效
     */
    public LocalDateTime getEffectiveAt() { return effectiveAt; }

    /**
     * 获取最后操作人.
     *
     * @return 操作人用户名
     */
    public String getOperator() { return operator; }

    /**
     * 获取变更原因.
     *
     * @return 变更原因描述
     */
    public String getReason() { return reason; }

    /**
     * 获取乐观锁版本号.
     *
     * @return 版本号
     */
    public int getVersion() { return version; }
}

