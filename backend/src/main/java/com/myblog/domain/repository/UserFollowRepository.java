package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.UserFollow;
import com.myblog.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 用户关注仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserFollowRepository {

    Optional<UserFollow> findByUsers(UserId followerUserId, UserId followingUserId);

    Optional<UserFollow> findByUsersIncludingDeleted(UserId followerUserId, UserId followingUserId);

    UserFollow save(UserFollow userFollow);

    Long nextId();

    boolean exists(UserId followerUserId, UserId followingUserId);

    int countFollowers(UserId userId);

    int countFollowing(UserId userId);

    List<Long> findFollowingUserIds(UserId followerUserId);
}
