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

    void insert(@Param("userId") Long userId,
                @Param("adId") Long adId,
                @Param("dismissedAt") LocalDateTime dismissedAt);

    List<Long> selectDismissedAdIds(@Param("userId") Long userId,
                                    @Param("since") LocalDateTime since);
}
