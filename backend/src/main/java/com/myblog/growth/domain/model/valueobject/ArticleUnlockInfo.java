package com.myblog.growth.domain.model.valueobject;

/**
 * 文章解锁所需的只读信息.
 */
public class ArticleUnlockInfo {

    private final Long id;
    private final boolean needUnlock;
    private final int unlockPointPrice;
    private final Long authorId;
    private final String status;

    private ArticleUnlockInfo(Long id, boolean needUnlock, int unlockPointPrice, Long authorId, String status) {
        this.id = id;
        this.needUnlock = needUnlock;
        this.unlockPointPrice = unlockPointPrice;
        this.authorId = authorId;
        this.status = status;
    }

    public static ArticleUnlockInfo of(Long id, boolean needUnlock, int unlockPointPrice,
                                       Long authorId, String status) {
        return new ArticleUnlockInfo(id, needUnlock, unlockPointPrice, authorId, status);
    }

    public Long getId() { return id; }
    public boolean isNeedUnlock() { return needUnlock; }
    public int getUnlockPointPrice() { return unlockPointPrice; }
    public Long getAuthorId() { return authorId; }
    public String getStatus() { return status; }
}
