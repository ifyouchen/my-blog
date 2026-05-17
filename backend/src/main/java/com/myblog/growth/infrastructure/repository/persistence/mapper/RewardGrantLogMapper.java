package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户奖励领取记录 MyBatis Mapper.
 *
 * @author czx
 * @since 2026-05-17
 */
@Mapper
public interface RewardGrantLogMapper {

    /**
     * 检查是否存在记录.
     */
    int countByUserAndReward(@Param("userId") Long userId,
                              @Param("rewardType") String rewardType,
                              @Param("rewardId") Long rewardId);

    /**
     * 插入新记录.
     */
    int insert(RewardGrantLog log);

    /**
     * 查询用户的所有领取记录.
     */
    List<RewardGrantLog> selectByUserId(@Param("userId") Long userId);

    /**
     * 管理端总数查询.
     */
    long countForAdmin(@Param("userId") Long userId, @Param("rewardType") String rewardType);

    /**
     * 管理端分页查询.
     */
    List<RewardGrantLog> selectPageForAdmin(@Param("userId") Long userId,
                                            @Param("rewardType") String rewardType,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);
}
