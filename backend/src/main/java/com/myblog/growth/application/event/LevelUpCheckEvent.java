package com.myblog.growth.application.event;

/**
 * 触发等级检查的内部事件.
 * <p>
 * 由 {@code ExpGrantAppService} 在经验入账后发布，
 * {@code LevelUpCheckEventHandler} 订阅并触发等级重算。
 * </p>
 */
public class LevelUpCheckEvent {

    /** 需要检查等级的用户 ID. */
    private final Long userId;

    /** 经验入账后的最新经验值（供等级计算服务直接使用，避免重复查库）. */
    private final int newExp;

    /**
     * 构造等级检查事件.
     *
     * @param userId 用户 ID
     * @param newExp 最新经验值
     */
    public LevelUpCheckEvent(Long userId, int newExp) {
        this.userId = userId;
        this.newExp = newExp;
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
     * 获取最新经验值.
     *
     * @return 最新经验值
     */
    public int getNewExp() {
        return newExp;
    }
}

