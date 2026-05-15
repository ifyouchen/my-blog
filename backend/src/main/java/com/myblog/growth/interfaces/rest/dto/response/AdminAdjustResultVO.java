package com.myblog.growth.interfaces.rest.dto.response;

/**
 * 管理员调分结果视图对象.
 * <p>对应 POST /api/admin/points/adjust 接口的响应体。</p>
 */
public class AdminAdjustResultVO {

    /** 被调分用户 ID. */
    private Long targetUserId;

    /** 调整后积分余额. */
    private int balanceAfter;

    /** 默认构造. */
    public AdminAdjustResultVO() {
    }

    /**
     * 带参构造.
     *
     * @param targetUserId 被调分用户 ID
     * @param balanceAfter 调整后积分余额
     */
    public AdminAdjustResultVO(Long targetUserId, int balanceAfter) {
        this.targetUserId = targetUserId;
        this.balanceAfter = balanceAfter;
    }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }
}

