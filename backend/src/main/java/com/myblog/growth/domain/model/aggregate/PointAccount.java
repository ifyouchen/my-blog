package com.myblog.growth.domain.model.aggregate;

import java.time.LocalDateTime;

/**
 * 用户积分账户聚合根.
 * <p>
 * 封装用户积分余额，所有状态变更必须通过业务方法进行，
 * 不对外暴露 setter，确保领域规则在聚合内部执行。
 * </p>
 *
 * <p>业务规则：</p>
 * <ul>
 *   <li>积分余额不能为负，扣款前必须先校验余额充足</li>
 *   <li>所有变更必须同步写入积分流水（PointJournal）</li>
 *   <li>乐观锁版本号每次 UPDATE 时自增，防止并发超扣</li>
 * </ul>
 */
public class PointAccount {

    /** 数据库主键 ID. */
    private Long id;

    /** 用户 ID，关联 blog_user.id. */
    private Long userId;

    /** 当前积分余额（不能为负）. */
    private int balance;

    /** 历史累计充值积分（仅用于统计展示）. */
    private int totalCharged;

    /** 历史累计消耗积分（仅用于统计展示）. */
    private int totalConsumed;

    /** 创建时间. */
    private LocalDateTime createdAt;

    /** 最后更新时间. */
    private LocalDateTime updatedAt;

    /** 乐观锁版本号，每次 UPDATE 自增. */
    private int version;

    /** 禁止外部直接构造，使用工厂方法. */
    private PointAccount() {
    }

    /**
     * 工厂方法：为新用户创建积分账户.
     * <p>初始余额 0，尚未持久化（id 为 null）。</p>
     *
     * @param userId 用户 ID
     * @return 未持久化的新积分账户
     */
    public static PointAccount create(Long userId) {
        PointAccount account = new PointAccount();
        account.userId = userId;
        account.balance = 0;
        account.totalCharged = 0;
        account.totalConsumed = 0;
        account.createdAt = LocalDateTime.now();
        account.updatedAt = LocalDateTime.now();
        account.version = 0;
        return account;
    }

    /**
     * 工厂方法：从持久化数据重建积分账户.
     *
     * @param id            主键 ID
     * @param userId        用户 ID
     * @param balance       当前余额
     * @param totalCharged  累计充值
     * @param totalConsumed 累计消耗
     * @param createdAt     创建时间
     * @param updatedAt     更新时间
     * @param version       乐观锁版本号
     * @return 重建后的积分账户
     */
    public static PointAccount restore(Long id, Long userId, int balance,
                                        int totalCharged, int totalConsumed,
                                        LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        PointAccount account = new PointAccount();
        account.id = id;
        account.userId = userId;
        account.balance = balance;
        account.totalCharged = totalCharged;
        account.totalConsumed = totalConsumed;
        account.createdAt = createdAt;
        account.updatedAt = updatedAt;
        account.version = version;
        return account;
    }

    /**
     * 增加积分（奖励、充值等正流水）.
     *
     * @param delta 积分增量（必须 &gt; 0）
     * @throws IllegalArgumentException delta ≤ 0 时抛出
     */
    public void credit(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("积分增量必须大于 0，实际值：" + delta);
        }
        this.balance += delta;
        this.totalCharged += delta;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 扣减积分（解锁等消耗场景）.
     *
     * @param delta 积分减量（必须 &gt; 0）
     * @throws IllegalStateException  余额不足时抛出
     * @throws IllegalArgumentException delta ≤ 0 时抛出
     */
    public void debit(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("积分减量必须大于 0，实际值：" + delta);
        }
        if (this.balance < delta) {
            throw new IllegalStateException("积分余额不足，余额=" + this.balance + "，需要=" + delta);
        }
        this.balance -= delta;
        this.totalConsumed += delta;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 管理员调整积分（正负均可，无余额限制检查）.
     * <p>注意：管理员调整为负数时可能导致余额为负，业务层需额外校验。</p>
     *
     * @param delta 积分变化量（正数=增加，负数=减少）
     */
    public void adjust(int delta) {
        this.balance += delta;
        if (delta > 0) {
            this.totalCharged += delta;
        } else {
            this.totalConsumed += (-delta);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /** 获取主键 ID. */
    public Long getId() { return id; }
    /** 获取用户 ID. */
    public Long getUserId() { return userId; }
    /** 获取当前积分余额. */
    public int getBalance() { return balance; }
    /** 获取历史累计充值积分. */
    public int getTotalCharged() { return totalCharged; }
    /** 获取历史累计消耗积分. */
    public int getTotalConsumed() { return totalConsumed; }
    /** 获取创建时间. */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** 获取最后更新时间. */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** 获取乐观锁版本号. */
    public int getVersion() { return version; }
}

