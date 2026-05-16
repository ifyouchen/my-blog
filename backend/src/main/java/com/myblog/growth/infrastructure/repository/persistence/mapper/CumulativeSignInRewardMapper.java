package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 累计签到里程碑奖励配置 MyBatis Mapper.
 *
 * @author czx
 * @since 2026-05-17
 */
@Mapper
public interface CumulativeSignInRewardMapper {

    /**
     * 查询所有启用的配置.
     */
    List<CumulativeSignInRewardConfig> selectAllEnabled();

    /**
     * 查询所有里程碑天数小于等于指定天数的配置.
     */
    List<CumulativeSignInRewardConfig> selectByMilestoneLessThanEqual(@Param("days") int days);

    /**
     * 更新配置.
     */
    int updateById(CumulativeSignInRewardConfig config);
}
