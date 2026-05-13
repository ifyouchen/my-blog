package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 用户取消关注事件.
 * <p>
 * 当用户取消关注其他用户时触发，用于更新关注/粉丝计数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UserUnfollowedEvent {

    /** 取消关注的用户ID（原粉丝）. */
    private final Long followerUserId;

    /** 被取消关注的用户ID. */
    private final Long followingUserId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造用户取消关注事件.
     *
     * @param followerUserId 取消关注的用户ID
     * @param followingUserId 被取消关注的用户ID
     */
    public UserUnfollowedEvent(Long followerUserId, Long followingUserId) {
        this.followerUserId = followerUserId;
        this.followingUserId = followingUserId;
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * 获取取消关注的用户ID.
     *
     * @return 用户ID
     */
    public Long getFollowerUserId() {
        return followerUserId;
    }

    /**
     * 获取被取消关注的用户ID.
     *
     * @return 用户ID
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