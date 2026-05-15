package com.myblog.growth.domain.model.aggregate;

import java.time.LocalDateTime;

/**
 * 用户成长账户聚合根.
 * <p>
 * 封装用户的经验值和等级数据，所有状态变更必须通过业务方法进行，
 * 不对外暴露 setter，确保领域规则在聚合内部执行。
 * </p>
 */
public class GrowthAccount {

    /** 数据库主键 ID. */
    private Long id;

    /** 用户 ID，关联 blog_user.id. */
    private Long userId;

    /** 当前累计经验值（只增不减）. */
    private int currentExp;

    /** 当前等级（1 起步）. */
    private int currentLevel;

    /** 创建时间. */
    private LocalDateTime createdAt;

    /** 最后更新时间. */
    private LocalDateTime updatedAt;

    /** 乐观锁版本号，每次 UPDATE 自增. */
    private int version;

    /** 禁止外部直接构造，使用工厂方法. */
    private GrowthAccount() {
    }

    /**
     * 工厂方法：为新用户创建成长账户.
     * <p>初始经验 0，初始等级 1，尚未持久化（id 为 null）。</p>
     *
     * @param userId 用户 ID
     * @return 未持久化的新成长账户
     */
    public static GrowthAccount create(Long userId) {
        GrowthAccount account = new GrowthAccount();
        account.userId = userId;
        account.currentExp = 0;
        account.currentLevel = 1;
        account.createdAt = LocalDateTime.now();
        account.updatedAt = LocalDateTime.now();
        account.version = 0;
        return account;
    }

    /**
     * 工厂方法：从持久化数据重建成长账户.
     *
     * @param id           主键 ID
     * @param userId       用户 ID
     * @param currentExp   当前经验值
     * @param currentLevel 当前等级
     * @param createdAt    创建时间
     * @param updatedAt    更新时间
     * @param version      乐观锁版本号
     * @return 重建后的成长账户
     */
    public static GrowthAccount restore(Long id, Long userId, int currentExp, int currentLevel,
                                        LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        GrowthAccount account = new GrowthAccount();
        account.id = id;
        account.userId = userId;
        account.currentExp = currentExp;
        account.currentLevel = currentLevel;
        account.createdAt = createdAt;
        account.updatedAt = updatedAt;
        account.version = version;
        return account;
    }

    /**
     * 增加经验值.
     * <p>delta ≤ 0 时直接忽略，不产生副作用。</p>
     *
     * @param delta 经验增量（应 > 0）
     */
    public void addExp(int delta) {
        if (delta <= 0) {
            return;
        }
        this.currentExp += delta;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 应用升级：将等级更新为 newLevel.
     * <p>
     * 只有 newLevel > currentLevel 时才发生升级（不支持降级）。
     * </p>
     *
     * @param newLevel 新等级
     * @return {@code true} 表示发生了升级，{@code false} 表示无变化
     */
    public boolean applyLevelUp(int newLevel) {
        if (newLevel > this.currentLevel) {
            this.currentLevel = newLevel;
            this.updatedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * 获取数据库主键 ID.
     *
     * @return 主键 ID（新建未持久化时为 null）
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取用户 ID.
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取当前累计经验值.
     *
     * @return 当前经验值
     */
    public int getCurrentExp() {
        return currentExp;
    }

    /**
     * 获取当前等级.
     *
     * @return 当前等级（≥ 1）
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 获取创建时间.
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取最后更新时间.
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取乐观锁版本号.
     *
     * @return 版本号
     */
    public int getVersion() {
        return version;
    }
}

