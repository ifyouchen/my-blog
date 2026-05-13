package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.UserFollow;
import com.myblog.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户关注仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface UserFollowRepository {

    /**
     * 根据关注双方查询有效的关注记录。
     *
     * @param followerUserId  发起关注的用户 ID（粉丝）
     * @param followingUserId 被关注的用户 ID（博主）
     * @return 关注记录 Optional
     */
    Optional<UserFollow> findByUsers(UserId followerUserId, UserId followingUserId);

    /**
     * 根据关注双方查询关注记录（包含已取消/逻辑删除的记录）。
     *
     * @param followerUserId  发起关注的用户 ID（粉丝）
     * @param followingUserId 被关注的用户 ID（博主）
     * @return 关注记录 Optional
     */
    Optional<UserFollow> findByUsersIncludingDeleted(UserId followerUserId, UserId followingUserId);

    /**
     * 保存关注记录（新增或更新）。
     *
     * @param userFollow 关注聚合根
     * @return 保存后的关注记录
     */
    UserFollow save(UserFollow userFollow);

    /**
     * 生成下一个关注记录 ID。
     *
     * @return 关注记录 ID
     */
    Long nextId();

    /**
     * 判断关注关系是否已存在（仅有效记录）。
     *
     * @param followerUserId  发起关注的用户 ID
     * @param followingUserId 被关注的用户 ID
     * @return 是否存在有效关注
     */
    boolean exists(UserId followerUserId, UserId followingUserId);

    /**
     * 统计用户的粉丝数量。
     *
     * @param userId 被关注的用户 ID
     * @return 粉丝数量
     */
    int countFollowers(UserId userId);

    /**
     * 批量统计多个用户的粉丝数量。
     *
     * @param userIds 用户 ID 列表
     * @return userId -> followerCount 映射
     */
    Map<Long, Integer> countFollowersBatch(List<Long> userIds);

    /**
     * 统计用户的关注数量。
     *
     * @param userId 发起关注的用户 ID
     * @return 关注数量
     */
    int countFollowing(UserId userId);

    /**
     * 查询用户关注的用户 ID 列表。
     *
     * @param followerUserId 发起关注的用户 ID
     * @return 被关注的用户 ID 列表
     */
    List<Long> findFollowingUserIds(UserId followerUserId);

    /**
     * 查询用户关注中且在候选列表内的用户 ID。
     *
     * @param followerUserId   发起关注的用户 ID
     * @param candidateUserIds 候选用户 ID 列表
     * @return 已关注的用户 ID 列表
     */
    List<Long> findFollowingUserIdsIn(UserId followerUserId, List<Long> candidateUserIds);

    /**
     * 查询指定用户的粉丝用户 ID 列表。
     *
     * @param followingUserId 被关注的用户 ID
     * @return 粉丝用户 ID 列表
     */
    List<Long> findFollowerUserIds(UserId followingUserId);
}
