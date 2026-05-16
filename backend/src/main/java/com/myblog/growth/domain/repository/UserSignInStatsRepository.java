package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.UserSignInStats;

import java.util.Optional;

/**
 * 用户签到统计 Repository 接口.
 *
 * @author czx
 * @since 2026-05-17
 */
public interface UserSignInStatsRepository {

    /**
     * 根据用户ID查询签到统计.
     *
     * @param userId 用户ID
     * @return 签到统计（可能为空）
     */
    Optional<UserSignInStats> findByUserId(Long userId);

    /**
     * 保存新的签到统计.
     *
     * @param stats 签到统计对象
     */
    void save(UserSignInStats stats);

    /**
     * 使用乐观锁更新签到统计.
     *
     * @param stats 签到统计对象
     * @return 更新行数
     */
    int updateWithCAS(UserSignInStats stats);
}
