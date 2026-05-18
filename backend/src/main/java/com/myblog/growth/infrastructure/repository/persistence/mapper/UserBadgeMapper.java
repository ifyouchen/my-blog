package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.UserBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户徽章 MyBatis Mapper.
 *
 * @author Codex
 * @since 1.0.0
 */
@Mapper
public interface UserBadgeMapper {

    /**
     * 幂等插入用户徽章.
     *
     * @param badge 用户徽章
     * @return 插入行数
     */
    int insertIgnore(UserBadge badge);

    /**
     * 统计用户是否拥有徽章.
     *
     * @param userId 用户ID
     * @param badgeCode 徽章编码
     * @return 数量
     */
    int countByUserIdAndBadgeCode(@Param("userId") Long userId, @Param("badgeCode") String badgeCode);

    /**
     * 查询用户已拥有徽章编码.
     *
     * @param userId 用户ID
     * @return 编码列表
     */
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户已拥有的启用徽章定义.
     *
     * @param userId 用户ID
     * @return 徽章定义列表
     */
    List<BadgeDefinition> selectOwnedDefinitions(@Param("userId") Long userId);
}
