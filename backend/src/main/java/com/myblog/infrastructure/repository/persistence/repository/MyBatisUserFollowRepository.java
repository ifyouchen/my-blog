package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.UserFollow;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.infrastructure.repository.persistence.entity.UserFollowDO;
import com.myblog.infrastructure.repository.persistence.mapper.UserFollowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户关注 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisUserFollowRepository implements UserFollowRepository {

    private final UserFollowMapper userFollowMapper;

    public MyBatisUserFollowRepository(UserFollowMapper userFollowMapper) {
        this.userFollowMapper = userFollowMapper;
    }

    @Override
    public Optional<UserFollow> findByUsers(UserId followerUserId, UserId followingUserId) {
        UserFollowDO userFollowDO = userFollowMapper.selectByUsers(followerUserId.getValue(), followingUserId.getValue());
        return userFollowDO == null ? Optional.<UserFollow>empty() : Optional.of(toDomain(userFollowDO));
    }

    @Override
    public Optional<UserFollow> findByUsersIncludingDeleted(UserId followerUserId, UserId followingUserId) {
        UserFollowDO userFollowDO = userFollowMapper.selectAnyByUsers(followerUserId.getValue(), followingUserId.getValue());
        return userFollowDO == null ? Optional.<UserFollow>empty() : Optional.of(toDomain(userFollowDO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserFollow save(UserFollow userFollow) {
        UserFollowDO userFollowDO = toData(userFollow);
        userFollowMapper.insertOrUpdate(userFollowDO);
        return userFollow;
    }

    @Override
    public Long nextId() {
        return userFollowMapper.selectNextId();
    }

    @Override
    public boolean exists(UserId followerUserId, UserId followingUserId) {
        return userFollowMapper.countByUsers(followerUserId.getValue(), followingUserId.getValue()) > 0;
    }

    @Override
    public int countFollowers(UserId userId) {
        return userFollowMapper.countFollowers(userId.getValue());
    }

    @Override
    public Map<Long, Integer> countFollowersBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Map<String, Object>> results = userFollowMapper.countFollowersBatch(userIds);
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : results) {
            Object userIdObj = row.get("userId");
            Object cntObj = row.get("cnt");
            if (userIdObj != null && cntObj != null) {
                Long userId = ((Number) userIdObj).longValue();
                Integer cnt = ((Number) cntObj).intValue();
                map.put(userId, cnt);
            }
        }
        return map;
    }

    @Override
    public int countFollowing(UserId userId) {
        return userFollowMapper.countFollowing(userId.getValue());
    }

    @Override
    public List<Long> findFollowingUserIds(UserId followerUserId) {
        return userFollowMapper.selectFollowingUserIds(followerUserId.getValue());
    }

    @Override
    public List<Long> findFollowingUserIdsIn(UserId followerUserId, List<Long> candidateUserIds) {
        if (candidateUserIds == null || candidateUserIds.isEmpty()) {
            return new java.util.ArrayList<Long>();
        }
        return userFollowMapper.selectFollowingUserIdsIn(followerUserId.getValue(), candidateUserIds);
    }

    private UserFollow toDomain(UserFollowDO userFollowDO) {
        return UserFollow.restore(
            userFollowDO.getId(),
            new UserId(userFollowDO.getFollowerUserId()),
            new UserId(userFollowDO.getFollowingUserId()),
            userFollowDO.getCreatedAt(),
            userFollowDO.getUpdatedAt(),
            userFollowDO.getDeletedAt(),
            userFollowDO.getVersion()
        );
    }

    private UserFollowDO toData(UserFollow userFollow) {
        UserFollowDO userFollowDO = new UserFollowDO();
        userFollowDO.setId(userFollow.getId().getValue());
        userFollowDO.setFollowerUserId(userFollow.getFollowerUserId().getValue());
        userFollowDO.setFollowingUserId(userFollow.getFollowingUserId().getValue());
        userFollowDO.setCreatedAt(userFollow.getCreatedAt());
        userFollowDO.setUpdatedAt(userFollow.getUpdatedAt());
        userFollowDO.setDeletedAt(userFollow.getDeletedAt());
        userFollowDO.setVersion(userFollow.getVersion());
        return userFollowDO;
    }
}
