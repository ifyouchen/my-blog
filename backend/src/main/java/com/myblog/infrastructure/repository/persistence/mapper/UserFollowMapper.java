package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.UserFollowDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户关注 Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface UserFollowMapper {

    /**
     * 查询有效的关注关系（未取消关注）。
     *
     * @param followerUserId  粉丝用户 ID
     * @param followingUserId 被关注用户 ID
     * @return 关注数据对象
     */
    UserFollowDO selectByUsers(@Param("followerUserId") Long followerUserId,
                               @Param("followingUserId") Long followingUserId);

    /**
     * 查询关注关系（包含已逻辑删除的记录）。
     *
     * @param followerUserId  粉丝用户 ID
     * @param followingUserId 被关注用户 ID
     * @return 关注数据对象
     */
    UserFollowDO selectAnyByUsers(@Param("followerUserId") Long followerUserId,
                                  @Param("followingUserId") Long followingUserId);

    /**
     * 统计有效关注关系数量。
     *
     * @param followerUserId  粉丝用户 ID
     * @param followingUserId 被关注用户 ID
     * @return 关注数量
     */
    int countByUsers(@Param("followerUserId") Long followerUserId,
                     @Param("followingUserId") Long followingUserId);

    /**
     * 统计用户的粉丝数量。
     *
     * @param userId 用户 ID
     * @return 粉丝数量
     */
    int countFollowers(@Param("userId") Long userId);

    /**
     * 批量统计多个用户的粉丝数量。
     *
     * @param userIds 用户 ID 列表
     * @return userId -> cnt 映射列表
     */
    List<Map<String, Object>> countFollowersBatch(@Param("userIds") List<Long> userIds);

    /**
     * 统计用户的关注数量。
     *
     * @param userId 用户 ID
     * @return 关注数量
     */
    int countFollowing(@Param("userId") Long userId);

    /**
     * 查询用户关注的所有用户 ID 列表。
     *
     * @param followerUserId 粉丝用户 ID
     * @return 被关注用户 ID 列表
     */
    List<Long> selectFollowingUserIds(@Param("followerUserId") Long followerUserId);

    /**
     * 从候选用户列表中查询该用户已关注的用户 ID。
     *
     * @param followerUserId   粉丝用户 ID
     * @param candidateUserIds 候选用户 ID 列表
     * @return 已关注的用户 ID 列表
     */
    List<Long> selectFollowingUserIdsIn(@Param("followerUserId") Long followerUserId,
                                        @Param("candidateUserIds") List<Long> candidateUserIds);

    /**
     * 查询用户的所有粉丝 ID 列表。
     *
     * @param followingUserId 被关注用户 ID
     * @return 粉丝用户 ID 列表
     */
    List<Long> selectFollowerUserIds(@Param("followingUserId") Long followingUserId);

    /**
     * 查询下一个关注记录 ID。
     *
     * @return 下一个关注记录 ID
     */
    Long selectNextId();

    /**
     * 插入关注记录。
     *
     * @param userFollowDO 关注数据对象
     * @return 影响行数
     */
    int insert(UserFollowDO userFollowDO);

    /**
     * 插入或更新关注记录（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param userFollowDO 关注数据对象
     * @return 影响行数
     */
    int insertOrUpdate(UserFollowDO userFollowDO);

    /**
     * 更新关注记录。
     *
     * @param userFollowDO 关注数据对象
     * @return 影响行数
     */
    int update(UserFollowDO userFollowDO);
}
