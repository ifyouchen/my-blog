package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotNull;

/**
 * 更新文章阅读权限与积分解锁规则请求。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UpdateArticleUnlockRuleRequest {

    /** 是否需要积分解锁。 */
    @NotNull(message = "请选择文章阅读权限")
    private Boolean needUnlock;

    /** 解锁所需积分。 */
    private Integer unlockPointPrice;

    /**
     * 获取是否需要积分解锁。
     *
     * @return true 表示需要积分解锁
     */
    public Boolean getNeedUnlock() {
        return needUnlock;
    }

    /**
     * 设置是否需要积分解锁。
     *
     * @param needUnlock 是否需要积分解锁
     */
    public void setNeedUnlock(Boolean needUnlock) {
        this.needUnlock = needUnlock;
    }

    /**
     * 获取解锁所需积分。
     *
     * @return 解锁积分
     */
    public Integer getUnlockPointPrice() {
        return unlockPointPrice;
    }

    /**
     * 设置解锁所需积分。
     *
     * @param unlockPointPrice 解锁积分
     */
    public void setUnlockPointPrice(Integer unlockPointPrice) {
        this.unlockPointPrice = unlockPointPrice;
    }
}
