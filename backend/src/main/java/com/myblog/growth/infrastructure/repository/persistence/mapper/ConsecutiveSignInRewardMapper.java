package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 连续签到奖励配置 MyBatis Mapper.
 *
 * @author czx
 * @since 2026-05-17
 */
@Mapper
public interface ConsecutiveSignInRewardMapper {

    /**
     * 查询所有启用的配置.
     */
    List<ConsecutiveSignInRewardConfig> selectAllEnabled();

    /**
     * 根据连续天数查找匹配的配置.
     */
    Optional<ConsecutiveSignInRewardConfig> selectByConsecutiveDays(@Param("consecutiveDays") int consecutiveDays);

    /**
     * 更新配置.
     */
    int updateById(ConsecutiveSignInRewardConfig config);
}
