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

    UserFollowDO selectByUsers(@Param("followerUserId") Long followerUserId,
                               @Param("followingUserId") Long followingUserId);

    UserFollowDO selectAnyByUsers(@Param("followerUserId") Long followerUserId,
                                  @Param("followingUserId") Long followingUserId);

    int countByUsers(@Param("followerUserId") Long followerUserId,
                     @Param("followingUserId") Long followingUserId);

    int countFollowers(@Param("userId") Long userId);

    List<Map<String, Object>> countFollowersBatch(@Param("userIds") List<Long> userIds);

    int countFollowing(@Param("userId") Long userId);

    List<Long> selectFollowingUserIds(@Param("followerUserId") Long followerUserId);

    List<Long> selectFollowingUserIdsIn(@Param("followerUserId") Long followerUserId,
                                        @Param("candidateUserIds") List<Long> candidateUserIds);

    List<Long> selectFollowerUserIds(@Param("followingUserId") Long followingUserId);

    Long selectNextId();

    int insert(UserFollowDO userFollowDO);
    int insertOrUpdate(UserFollowDO userFollowDO);


    int update(UserFollowDO userFollowDO);
}
