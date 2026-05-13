package com.myblog.domain.repository;

import com.myblog.infrastructure.repository.persistence.entity.LearningProgressDO;

/**
 * 学习进度仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface LearningProgressRepository {

    /**
     * 根据用户和资产查询学习进度。
     *
     * @param userId    用户 ID
     * @param assetType 资产类型（COLUMN / TOPIC 等）
     * @param assetId   资产 ID
     * @return 学习进度数据对象，不存在时返回 null
     */
    LearningProgressDO findByUserAndAsset(Long userId, String assetType, Long assetId);

    /**
     * 保存学习进度（新增或更新）。
     *
     * @param progress 学习进度数据对象
     */
    void save(LearningProgressDO progress);

    /**
     * 生成下一个学习进度记录 ID。
     *
     * @return 学习进度记录 ID
     */
    Long nextId();
}
