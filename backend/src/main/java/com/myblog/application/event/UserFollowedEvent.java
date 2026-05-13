package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 用户关注事件.
 * <p>
 * 当用户关注其他用户时触发，用于更新关注/粉丝计数和发送关注通知.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UserFollowedEvent {

    /** 关注者用户ID（粉丝）. */
    private final Long followerUserId;

    /** 被关注者用户ID. */
    private final Long followingUserId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造用户关注事件.
     *
     * @param followerUserId 关注者用户ID
     * @param followingUserId 被关注者用户ID
     */
    public UserFollowedEvent(Long followerUserId, Long followingUserId) {
        this.followerUserId = followerUserId;
        this.followingUserId = followingUserId;
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * 获取关注者用户ID.
     *
     * @return 关注者用户ID
     */
    public Long getFollowerUserId() {
        return followerUserId;
    }

    /**
     * 获取被关注者用户ID.
     *
     * @return 被关注者用户ID
     */
    public Long getFollowingUserId() {
        return followingUserId;
    }

    /**
     * 获取事件发生时间.
     *
     * @return 事件发生时间
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}