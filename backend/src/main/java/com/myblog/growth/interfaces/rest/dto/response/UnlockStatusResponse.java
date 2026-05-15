package com.myblog.growth.interfaces.rest.dto.response;

/**
 * 文章解锁状态响应 DTO.
 * <p>对应 GET /api/articles/{id}/unlock-status 接口的响应体。</p>
 */
public class UnlockStatusResponse {

    private Long articleId;
    private boolean needUnlock;
    private int unlockPointPrice;
    private boolean unlocked;
    private int currentBalance;
    private String reason;

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

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
