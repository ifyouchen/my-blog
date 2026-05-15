package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户积分账户数据库实体.
 * <p>对应表 {@code user_point_account}。</p>
 */
public class UserPointAccountDO {

    /** 主键 ID. */
    private Long id;
    /** 用户 ID. */
    private Long userId;
    /** 当前可用积分余额. */
    private int balance;
    /** 累计获得积分（total_earned）. */
    private int totalEarned;
    /** 累计消耗积分（total_spent）. */
    private int totalSpent;
    /** 创建时间. */
    private LocalDateTime createdAt;
    /** 更新时间. */
    private LocalDateTime updatedAt;
    /** 软删除时间. */
    private LocalDateTime deletedAt;
    /** 乐观锁版本号. */
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
    public int getTotalEarned() { return totalEarned; }
    public void setTotalEarned(int totalEarned) { this.totalEarned = totalEarned; }
    public int getTotalSpent() { return totalSpent; }
    public void setTotalSpent(int totalSpent) { this.totalSpent = totalSpent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}

