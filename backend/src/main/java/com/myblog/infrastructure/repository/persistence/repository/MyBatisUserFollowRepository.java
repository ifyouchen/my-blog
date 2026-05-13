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

    /**
     * 创建用户关注 MyBatis 仓储。
     *
     * @param userFollowMapper 用户关注 Mapper
     */
    public MyBatisUserFollowRepository(UserFollowMapper userFollowMapper) {
        this.userFollowMapper = userFollowMapper;
    }

    /**
     * 查询有效的关注关系（未取消）。
     *
     * @param followerUserId  粉丝用户 ID
     * @param followingUserId 被关注用户 ID
     * @return 关注 Optional
     */
    @Override
    public Optional<UserFollow> findByUsers(UserId followerUserId, UserId followingUserId) {
        UserFollowDO userFollowDO = userFollowMapper.selectByUsers(followerUserId.getValue(), followingUserId.getValue());
        return userFollowDO == null ? Optional.<UserFollow>empty() : Optional.of(toDomain(userFollowDO));
    }

    /**
     * 查询关注关系（包含已逻辑删除的记录）。
     *
     * @param followerUserId  粉丝用户 ID
     * @param followingUserId 被关注用户 ID
     * @return 关注 Optional
     */
    @Override
    public Optional<UserFollow> findByUsersIncludingDeleted(UserId followerUserId, UserId followingUserId) {
        UserFollowDO userFollowDO = userFollowMapper.selectAnyByUsers(followerUserId.getValue(), followingUserId.getValue());
        return userFollowDO == null ? Optional.<UserFollow>empty() : Optional.of(toDomain(userFollowDO));
    }

    /**
     * 保存关注记录。
     *
     * @param userFollow 关注聚合根
     * @return 保存后的关注聚合根
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserFollow save(UserFollow userFollow) {
        UserFollowDO userFollowDO = toData(userFollow);
        userFollowMapper.insertOrUpdate(userFollowDO);
        return userFollow;
    }

    /**
     * 生成下一个关注记录 ID。
     *
     * @return 关注记录 ID
     */
    @Override
    public Long nextId() {
        return userFollowMapper.selectNextId();
    }

    /**
     * 判断有效的关注关系是否存在。
     *
     * @param followerUserId  粉丝用户 ID
     * @param followingUserId 被关注用户 ID
     * @return 是否已关注
     */
    @Override
    public boolean exists(UserId followerUserId, UserId followingUserId) {
        return userFollowMapper.countByUsers(followerUserId.getValue(), followingUserId.getValue()) > 0;
    }

    /**
     * 统计用户的粉丝数量。
     *
     * @param userId 用户 ID
     * @return 粉丝数量
     */
    @Override
    public int countFollowers(UserId userId) {
        return userFollowMapper.countFollowers(userId.getValue());
    }

    /**
     * 批量统计多个用户的粉丝数量。
     *
     * @param userIds 用户 ID 列表
     * @return userId -> 粉丝数量 映射
     */
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

    /**
     * 统计用户的关注数量。
     *
     * @param userId 用户 ID
     * @return 关注数量
     */
    @Override
    public int countFollowing(UserId userId) {
        return userFollowMapper.countFollowing(userId.getValue());
    }

    /**
     * 查询用户关注的所有用户 ID 列表。
     *
     * @param followerUserId 粉丝用户 ID
     * @return 被关注用户 ID 列表
     */
    @Override
    public List<Long> findFollowingUserIds(UserId followerUserId) {
        return userFollowMapper.selectFollowingUserIds(followerUserId.getValue());
    }

    /**
     * 从候选用户列表中查询该用户已关注的用户 ID。
     *
     * @param followerUserId   粉丝用户 ID
     * @param candidateUserIds 候选用户 ID 列表
     * @return 已关注的用户 ID 列表
     */
    @Override
    public List<Long> findFollowingUserIdsIn(UserId followerUserId, List<Long> candidateUserIds) {
        if (candidateUserIds == null || candidateUserIds.isEmpty()) {
            return new java.util.ArrayList<Long>();
        }
        return userFollowMapper.selectFollowingUserIdsIn(followerUserId.getValue(), candidateUserIds);
    }

    /**
     * 查询用户的所有粉丝 ID 列表。
     *
     * @param followingUserId 被关注用户 ID
     * @return 粉丝用户 ID 列表
     */
    @Override
    public List<Long> findFollowerUserIds(UserId followingUserId) {
        return userFollowMapper.selectFollowerUserIds(followingUserId.getValue());
    }

    /**
     * 将 DO 转换为领域对象。
     *
     * @param userFollowDO 关注数据对象
     * @return 关注聚合根
     */
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

    /**
     * 将领域对象转换为 DO。
     *
     * @param userFollow 关注聚合根
     * @return 关注数据对象
     */
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
