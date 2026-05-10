package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.LearningProgressDO;
import org.apache.ibatis.annotations.Param;

/**
 * 学习进度 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface LearningProgressMapper {

    LearningProgressDO selectByUserAndAsset(@Param("userId") Long userId,
                                            @Param("assetType") String assetType,
                                            @Param("assetId") Long assetId);

    int insert(LearningProgressDO progress);

    int update(LearningProgressDO progress);

    Long selectNextId();
}
