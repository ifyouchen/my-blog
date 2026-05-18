package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.UserSignInStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户签到统计 MyBatis Mapper.
 *
 * @author czx
 * @since 2026-05-17
 */
@Mapper
public interface UserSignInStatsMapper {

    /**
     * 根据用户ID查询.
     */
    Optional<UserSignInStats> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID批量查询.
     */
    List<UserSignInStats> selectByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 插入新记录.
     */
    int insert(UserSignInStats stats);

    /**
     * 使用乐观锁更新.
     */
    int updateWithCAS(UserSignInStats stats);
}
