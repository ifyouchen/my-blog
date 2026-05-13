package com.myblog.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户广告关闭记录 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
@Mapper
public interface AdDismissalMapper {

    /**
     * 插入广告关闭记录。
     *
     * @param userId      用户 ID
     * @param adId        广告 ID
     * @param dismissedAt 关闭时间
     */
    void insert(@Param("userId") Long userId,
                @Param("adId") Long adId,
                @Param("dismissedAt") LocalDateTime dismissedAt);

    /**
     * 查询用户已关闭的广告 ID 列表。
     *
     * @param userId 用户 ID
     * @param since  查询起始时间
     * @return 已关闭的广告 ID 列表
     */
    List<Long> selectDismissedAdIds(@Param("userId") Long userId,
                                    @Param("since") LocalDateTime since);
}
