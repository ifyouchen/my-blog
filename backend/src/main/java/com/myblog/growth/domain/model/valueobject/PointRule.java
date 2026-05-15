package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 积分规则值对象.
 * <p>
 * 对应数据库 {@code point_rule_config} 表，描述某一行为类型每次可获得的积分量
 * 及每日上限策略。值对象不可变，通过工厂方法构建。
 * </p>
 */
public class PointRule {

    /** 数据库主键 ID. */
    private Long id;

    /**
     * 来源类型（对应 PointEventType 枚举值），对应数据库字段 {@code source_type}.
     */
    private String sourceType;

    /** 单次奖励积分量（负数表示消耗）. */
    private int pointAmount;

    /**
     * 每日上限（0 = 无限制）.
     * <p>对于 SIGN_IN、INVITE 等上限次数固定为 1 的场景，dailyLimit=1；
     * RECHARGE 无上限时 dailyLimit=0。</p>
     */
    private int dailyLimit;

    /** 每日限额策略（TOTAL / COUNT，含义同经验规则）. */
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

    /** 软删除时间（null = 未删除）. */
    private LocalDateTime deletedAt;

    /** 禁止外部直接构造，使用工厂方法. */
    private PointRule() {
    }

    /**
     * 工厂方法：构建积分规则值对象.
     *
     * @param id                  主键 ID
     * @param sourceType          行为类型
     * @param pointAmount         单次奖励积分量
     * @param dailyLimit          每日上限（0=无上限）
     * @param dailyLimitStrategy  限额策略
     * @param enabled             是否启用
     * @param effectiveAt         生效时间
     * @param operator            最后操作人
     * @param reason              变更原因
     * @param version             乐观锁版本号
     * @param deletedAt           软删除时间
     * @return 积分规则值对象
     */
    public static PointRule of(Long id, String sourceType, int pointAmount,
                               int dailyLimit, String dailyLimitStrategy, boolean enabled,
                               LocalDateTime effectiveAt, String operator, String reason, int version,
                               LocalDateTime deletedAt) {
        PointRule rule = new PointRule();
        rule.id = id;
        rule.sourceType = sourceType;
        rule.pointAmount = pointAmount;
        rule.dailyLimit = dailyLimit;
        rule.dailyLimitStrategy = dailyLimitStrategy;
        rule.enabled = enabled;
        rule.effectiveAt = effectiveAt;
        rule.operator = operator;
        rule.reason = reason;
        rule.version = version;
        rule.deletedAt = deletedAt;
        return rule;
    }

    /**
     * 判断是否已软删除.
     *
     * @return {@code true} 表示已删除
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 判断规则当前是否已生效.
     * <p>必须 enabled=true，且 effectiveAt 为 null 或已到达。</p>
     *
     * @return {@code true} 表示已生效
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

    /** 获取主键 ID. */
    public Long getId() { return id; }
    /** 获取来源类型. */
    public String getSourceType() { return sourceType; }
    /** 获取单次奖励积分量. */
    public int getPointAmount() { return pointAmount; }
    /** 获取每日上限. */
    public int getDailyLimit() { return dailyLimit; }
    /** 获取限额策略. */
    public String getDailyLimitStrategy() { return dailyLimitStrategy; }
    /** 获取是否启用. */
    public boolean isEnabled() { return enabled; }
    /** 获取生效时间. */
    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    /** 获取操作人. */
    public String getOperator() { return operator; }
    /** 获取变更原因. */
    public String getReason() { return reason; }
    /** 获取版本号. */
    public int getVersion() { return version; }
    /** 获取软删除时间. */
    public LocalDateTime getDeletedAt() { return deletedAt; }
}

