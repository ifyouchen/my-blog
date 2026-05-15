package com.myblog.growth.infrastructure.repository.persistence.converter;

import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.aggregate.PointAccount;
import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.infrastructure.repository.persistence.entity.GrowthRuleConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.entity.LevelThresholdConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.entity.PointRuleConfigDO;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserExpJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserGrowthAccountDO;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserPointAccountDO;
import com.myblog.growth.infrastructure.repository.persistence.entity.UserPointJournalDO;

/**
 * 成长模块 DO ↔ Domain 对象转换工具类.
 * <p>
 * 纯静态转换方法，不依赖 Spring Bean，无状态，线程安全。
 * </p>
 */
public final class GrowthConverter {

    /** 禁止实例化. */
    private GrowthConverter() {
    }

    // ─────────────────────────── GrowthAccount ───────────────────────────

    /**
     * 将 {@link UserGrowthAccountDO} 转换为领域聚合根 {@link GrowthAccount}.
     *
     * @param do_ 数据库实体，不能为 null
     * @return 领域聚合根
     */
    public static GrowthAccount toDomain(UserGrowthAccountDO do_) {
        return GrowthAccount.restore(
                do_.getId(),
                do_.getUserId(),
                do_.getCurrentExp(),
                do_.getCurrentLevel(),
                do_.getCreatedAt(),
                do_.getUpdatedAt(),
                do_.getVersion()
        );
    }

    /**
     * 将领域聚合根 {@link GrowthAccount} 转换为 {@link UserGrowthAccountDO}（用于 INSERT）.
     *
     * @param account 领域聚合根，不能为 null
     * @return 数据库实体
     */
    public static UserGrowthAccountDO toDO(GrowthAccount account) {
        UserGrowthAccountDO do_ = new UserGrowthAccountDO();
        do_.setId(account.getId());
        do_.setUserId(account.getUserId());
        do_.setCurrentExp(account.getCurrentExp());
        do_.setCurrentLevel(account.getCurrentLevel());
        do_.setCreatedAt(account.getCreatedAt());
        do_.setUpdatedAt(account.getUpdatedAt());
        do_.setVersion(account.getVersion());
        return do_;
    }

    // ─────────────────────────── ExpJournal ──────────────────────────────

    /**
     * 将 {@link UserExpJournalDO} 转换为值对象 {@link ExpJournal}.
     *
     * @param do_ 数据库实体，不能为 null
     * @return 经验流水值对象
     */
    public static ExpJournal toDomain(UserExpJournalDO do_) {
        return ExpJournal.restore(
                do_.getId(),
                do_.getUserId(),
                do_.getDelta(),
                do_.getBalanceAfter(),
                do_.getEventType(),
                do_.getSourceId(),
                do_.getRemark(),
                do_.getIdempotentKey(),
                do_.getCreatedAt(),
                do_.getVersion()
        );
    }

    /**
     * 将值对象 {@link ExpJournal} 转换为 {@link UserExpJournalDO}（用于 INSERT）.
     *
     * @param journal 经验流水值对象，不能为 null
     * @return 数据库实体
     */
    public static UserExpJournalDO toDO(ExpJournal journal) {
        UserExpJournalDO do_ = new UserExpJournalDO();
        do_.setId(journal.getId());
        do_.setUserId(journal.getUserId());
        do_.setDelta(journal.getDelta());
        do_.setBalanceAfter(journal.getBalanceAfter());
        do_.setEventType(journal.getEventType());
        do_.setSourceId(journal.getSourceId());
        do_.setRemark(journal.getRemark());
        do_.setIdempotentKey(journal.getIdempotentKey());
        do_.setCreatedAt(journal.getCreatedAt());
        do_.setVersion(journal.getVersion());
        return do_;
    }

    // ─────────────────────────── GrowthRule ──────────────────────────────

    /**
     * 将 {@link GrowthRuleConfigDO} 转换为值对象 {@link GrowthRule}.
     *
     * @param do_ 数据库实体，不能为 null
     * @return 经验规则值对象
     */
    public static GrowthRule toDomain(GrowthRuleConfigDO do_) {
        return GrowthRule.of(
                do_.getId(),
                do_.getEventType(),
                do_.getRole(),
                do_.getExpAmount(),
                do_.getDailyLimit(),
                do_.getDailyLimitStrategy(),
                Boolean.TRUE.equals(do_.getEnabled()),
                do_.getEffectiveAt(),
                do_.getOperator(),
                do_.getReason(),
                do_.getVersion()
        );
    }

    /**
     * 将值对象 {@link GrowthRule} 转换为 {@link GrowthRuleConfigDO}（用于 INSERT / UPDATE）.
     *
     * @param rule 经验规则值对象，不能为 null
     * @return 数据库实体
     */
    public static GrowthRuleConfigDO toDO(GrowthRule rule) {
        GrowthRuleConfigDO do_ = new GrowthRuleConfigDO();
        do_.setId(rule.getId());
        do_.setEventType(rule.getEventType());
        do_.setRole(rule.getRole());
        do_.setExpAmount(rule.getExpAmount());
        do_.setDailyLimit(rule.getDailyLimit());
        do_.setDailyLimitStrategy(rule.getDailyLimitStrategy());
        do_.setEnabled(rule.isEnabled());
        do_.setEffectiveAt(rule.getEffectiveAt());
        do_.setOperator(rule.getOperator());
        do_.setReason(rule.getReason());
        do_.setVersion(rule.getVersion());
        return do_;
    }

    // ─────────────────────────── LevelThreshold ──────────────────────────

    /**
     * 将 {@link LevelThresholdConfigDO} 转换为值对象 {@link LevelThreshold}.
     *
     * @param do_ 数据库实体，不能为 null
     * @return 等级阈值值对象
     */
    public static LevelThreshold toDomain(LevelThresholdConfigDO do_) {
        return LevelThreshold.of(
                do_.getId(),
                do_.getLevel(),
                do_.getMinExp(),
                do_.getLevelName(),
                do_.getDescription(),
                Boolean.TRUE.equals(do_.getEnabled()),
                do_.getOperator(),
                do_.getVersion()
        );
    }

    /**
     * 将值对象 {@link LevelThreshold} 转换为 {@link LevelThresholdConfigDO}（用于批量保存）.
     *
     * @param threshold 等级阈值值对象，不能为 null
     * @return 数据库实体
     */
    public static LevelThresholdConfigDO toDO(LevelThreshold threshold) {
        LevelThresholdConfigDO do_ = new LevelThresholdConfigDO();
        do_.setId(threshold.getId());
        do_.setLevel(threshold.getLevel());
        do_.setMinExp(threshold.getMinExp());
        do_.setLevelName(threshold.getLevelName());
        do_.setDescription(threshold.getDescription());
        do_.setEnabled(threshold.isEnabled());
        do_.setOperator(threshold.getOperator());
        do_.setVersion(threshold.getVersion());
        return do_;
    }

    // ─────────────────────────── PointAccount ────────────────────────────

    /**
     * 将 {@link UserPointAccountDO} 转换为领域聚合根 {@link PointAccount}.
     *
     * @param do_ 数据库实体，不能为 null
     * @return 领域聚合根
     */
    public static PointAccount toPointAccountDomain(UserPointAccountDO do_) {
        return PointAccount.restore(
                do_.getId(),
                do_.getUserId(),
                do_.getBalance(),
                do_.getTotalEarned(),
                do_.getTotalSpent(),
                do_.getCreatedAt(),
                do_.getUpdatedAt(),
                do_.getVersion()
        );
    }

    /**
     * 将领域聚合根 {@link PointAccount} 转换为 {@link UserPointAccountDO}（用于 INSERT / CAS UPDATE）.
     *
     * @param account 领域聚合根，不能为 null
     * @return 数据库实体
     */
    public static UserPointAccountDO toPointAccountDO(PointAccount account) {
        UserPointAccountDO do_ = new UserPointAccountDO();
        do_.setId(account.getId());
        do_.setUserId(account.getUserId());
        do_.setBalance(account.getBalance());
        do_.setTotalEarned(account.getTotalEarned());
        do_.setTotalSpent(account.getTotalSpent());
        do_.setCreatedAt(account.getCreatedAt());
        do_.setUpdatedAt(account.getUpdatedAt());
        do_.setVersion(account.getVersion());
        return do_;
    }

    // ─────────────────────────── PointJournal ────────────────────────────

    /**
     * 将 {@link UserPointJournalDO} 转换为值对象 {@link PointJournal}.
     *
     * @param do_ 数据库实体，不能为 null
     * @return 积分流水值对象
     */
    public static PointJournal toPointJournalDomain(UserPointJournalDO do_) {
        return PointJournal.restore(
                do_.getId(),
                do_.getUserId(),
                do_.getDelta(),
                do_.getBalanceAfter(),
                do_.getSourceType(),
                do_.getBizNo(),
                do_.getRemark(),
                do_.getOperator(),
                do_.getCreatedAt(),
                do_.getVersion()
        );
    }

    /**
     * 将值对象 {@link PointJournal} 转换为 {@link UserPointJournalDO}（用于 INSERT IGNORE）.
     *
     * @param journal 积分流水值对象，不能为 null
     * @return 数据库实体
     */
    public static UserPointJournalDO toPointJournalDO(PointJournal journal) {
        UserPointJournalDO do_ = new UserPointJournalDO();
        do_.setId(journal.getId());
        do_.setUserId(journal.getUserId());
        do_.setDelta(journal.getDelta());
        do_.setBalanceAfter(journal.getBalanceAfter());
        do_.setSourceType(journal.getSourceType());
        do_.setBizNo(journal.getBizNo());
        do_.setRemark(journal.getRemark());
        do_.setOperator(journal.getOperator());
        do_.setCreatedAt(journal.getCreatedAt());
        do_.setVersion(journal.getVersion());
        return do_;
    }

    // ─────────────────────────── PointRule ───────────────────────────────

    /**
     * 将 {@link PointRuleConfigDO} 转换为值对象 {@link PointRule}.
     *
     * @param do_ 数据库实体，不能为 null
     * @return 积分规则值对象
     */
    public static PointRule toPointRuleDomain(PointRuleConfigDO do_) {
        return PointRule.of(
                do_.getId(),
                do_.getSourceType(),
                do_.getPointAmount(),
                do_.getDailyLimit(),
                null,
                Boolean.TRUE.equals(do_.getEnabled()),
                null,
                do_.getOperator(),
                do_.getReason(),
                do_.getVersion(),
                do_.getDeletedAt()
        );
    }

    /**
     * 将值对象 {@link PointRule} 转换为 {@link PointRuleConfigDO}（用于 INSERT / UPDATE）.
     *
     * @param rule 积分规则值对象，不能为 null
     * @return 数据库实体
     */
    public static PointRuleConfigDO toPointRuleDO(PointRule rule) {
        PointRuleConfigDO do_ = new PointRuleConfigDO();
        do_.setId(rule.getId());
        do_.setSourceType(rule.getSourceType());
        do_.setPointAmount(rule.getPointAmount());
        do_.setDailyLimit(rule.getDailyLimit());
        do_.setEnabled(rule.isEnabled());
        do_.setOperator(rule.getOperator());
        do_.setReason(rule.getReason());
        do_.setVersion(rule.getVersion());
        return do_;
    }
}

