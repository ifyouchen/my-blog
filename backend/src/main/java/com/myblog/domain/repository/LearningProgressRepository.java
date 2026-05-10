package com.myblog.domain.repository;

import com.myblog.infrastructure.repository.persistence.entity.LearningProgressDO;

/**
 * 学习进度仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface LearningProgressRepository {

    LearningProgressDO findByUserAndAsset(Long userId, String assetType, Long assetId);

    void save(LearningProgressDO progress);

    Long nextId();
}
