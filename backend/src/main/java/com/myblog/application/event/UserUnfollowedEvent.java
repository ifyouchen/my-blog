package com.myblog.application.event;

import java.time.LocalDateTime;

public class UserUnfollowedEvent {
    private final Long followerUserId;
    private final Long followingUserId;
    private final LocalDateTime occurredOn;

    public UserUnfollowedEvent(Long followerUserId, Long followingUserId) {
        this.followerUserId = followerUserId;
        this.followingUserId = followingUserId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getFollowerUserId() {
        return followerUserId;
    }

    public Long getFollowingUserId() {
        return followingUserId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}