package com.myblog.growth.interfaces.rest.dto.response;

/**
 * 用户积分账户视图对象（只读展示）.
 * <p>对应 GET /api/points/account 接口的响应体。</p>
 */
public class PointAccountVO {

    /** 用户 ID. */
    private Long userId;

    /** 当前可用积分余额. */
    private int balance;

    /** 累计获得积分. */
    private int totalEarned;

    /** 累计消耗积分. */
    private int totalSpent;

    /** 默认构造. */
    public PointAccountVO() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }

    public int getTotalEarned() { return totalEarned; }
    public void setTotalEarned(int totalEarned) { this.totalEarned = totalEarned; }

    public int getTotalSpent() { return totalSpent; }
    public void setTotalSpent(int totalSpent) { this.totalSpent = totalSpent; }
}

