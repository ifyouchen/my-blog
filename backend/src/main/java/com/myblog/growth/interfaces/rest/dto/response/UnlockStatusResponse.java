package com.myblog.growth.interfaces.rest.dto.response;

/**
 * 文章解锁状态响应 DTO.
 * <p>对应 GET /api/articles/{id}/unlock-status 接口的响应体。</p>
 */
public class UnlockStatusResponse {

    /** 文章 ID. */
    private Long articleId;

    /** 是否需要解锁（false=免费文章）. */
    private boolean needUnlock;

    /** 解锁所需积分数. */
    private int unlockPointPrice;

    /** 当前用户是否已解锁. */
    private boolean unlocked;

    /** 当前用户积分余额. */
    private int currentBalance;

    /** 默认构造. */
    public UnlockStatusResponse() {
    }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public boolean isNeedUnlock() { return needUnlock; }
    public void setNeedUnlock(boolean needUnlock) { this.needUnlock = needUnlock; }

    public int getUnlockPointPrice() { return unlockPointPrice; }
    public void setUnlockPointPrice(int unlockPointPrice) { this.unlockPointPrice = unlockPointPrice; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public int getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(int currentBalance) { this.currentBalance = currentBalance; }
}

