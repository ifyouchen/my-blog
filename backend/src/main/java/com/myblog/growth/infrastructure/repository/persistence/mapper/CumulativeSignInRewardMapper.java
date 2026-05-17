package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 累计签到里程碑奖励配置 MyBatis Mapper.
 *
 * @author czx
 * @since 2026-05-17
 */
@Mapper
public interface CumulativeSignInRewardMapper {

    /**
     * 根据主键查询.
     */
    Optional<CumulativeSignInRewardConfig> selectById(@Param("id") Long id);

    /**
     * 查询所有启用的配置.
     */
    List<CumulativeSignInRewardConfig> selectAllEnabled();

    /**
     * 查询全部未删除配置.
     */
    List<CumulativeSignInRewardConfig> selectAll();

    /**
     * 查询所有里程碑天数小于等于指定天数的配置.
     */
    List<CumulativeSignInRewardConfig> selectByMilestoneLessThanEqual(@Param("days") int days);

    /**
     * 根据里程碑天数查询已软删除配置.
     */
    Optional<CumulativeSignInRewardConfig> selectDeletedByMilestoneDays(@Param("milestoneDays") int milestoneDays);

    /**
     * 更新配置.
     */
    int updateById(CumulativeSignInRewardConfig config);

    /**
     * 插入配置.
     */
    int insert(CumulativeSignInRewardConfig config);

    /**
     * 恢复软删除配置.
     */
    int restoreById(CumulativeSignInRewardConfig config);

    /**
     * 软删除配置.
     */
    int softDelete(@Param("id") Long id, @Param("version") int version);
}
