package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.repository.LearningProgressRepository;
import com.myblog.infrastructure.repository.persistence.entity.LearningProgressDO;
import com.myblog.infrastructure.repository.persistence.mapper.LearningProgressMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 学习进度 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisLearningProgressRepository implements LearningProgressRepository {

    private final LearningProgressMapper learningProgressMapper;

    public MyBatisLearningProgressRepository(LearningProgressMapper learningProgressMapper) {
        this.learningProgressMapper = learningProgressMapper;
    }

    @Override
    public LearningProgressDO findByUserAndAsset(Long userId, String assetType, Long assetId) {
        return learningProgressMapper.selectByUserAndAsset(userId, assetType, assetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(LearningProgressDO progress) {
        LocalDateTime now = LocalDateTime.now();
        if (progress.getCreatedAt() == null) {
            progress.setCreatedAt(now);
        }
        progress.setUpdatedAt(now);
        if (progress.getVersion() == null) {
            progress.setVersion(0);
        }
        if (progress.getId() == null) {
            progress.setId(nextId());
            learningProgressMapper.insert(progress);
            return;
        }
        learningProgressMapper.update(progress);
    }

    @Override
    public Long nextId() {
        return learningProgressMapper.selectNextId();
    }
}
