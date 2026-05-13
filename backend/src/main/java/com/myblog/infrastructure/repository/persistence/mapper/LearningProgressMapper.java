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

    /**
     * 根据用户和资产查询学习进度。
     *
     * @param userId    用户 ID
     * @param assetType 资产类型（如 TOPIC、COLUMN）
     * @param assetId   资产 ID
     * @return 学习进度数据对象
     */
    LearningProgressDO selectByUserAndAsset(@Param("userId") Long userId,
                                            @Param("assetType") String assetType,
                                            @Param("assetId") Long assetId);

    /**
     * 插入学习进度记录。
     *
     * @param progress 学习进度数据对象
     * @return 影响行数
     */
    int insert(LearningProgressDO progress);

    /**
     * 更新学习进度记录。
     *
     * @param progress 学习进度数据对象
     * @return 影响行数
     */
    int update(LearningProgressDO progress);

    /**
     * 查询下一个学习进度记录 ID。
     *
     * @return 下一个学习进度记录 ID
     */
    Long selectNextId();
}
