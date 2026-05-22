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
    private final IdGenerator idGenerator;

    /**
     * 创建学习进度 MyBatis 仓储。
     *
     * @param learningProgressMapper 学习进度 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisLearningProgressRepository(LearningProgressMapper learningProgressMapper, IdGenerator idGenerator) {
        this.learningProgressMapper = learningProgressMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据用户和资产查询学习进度。
     *
     * @param userId    用户 ID
     * @param assetType 资产类型（如 TOPIC、COLUMN）
     * @param assetId   资产 ID
     * @return 学习进度数据对象
     */
    @Override
    public LearningProgressDO findByUserAndAsset(Long userId, String assetType, Long assetId) {
        return learningProgressMapper.selectByUserAndAsset(userId, assetType, assetId);
    }

    /**
     * 保存学习进度（新记录插入，已有记录更新）。
     *
     * @param progress 学习进度数据对象
     */
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

    /**
     * 生成下一个学习进度记录 ID。
     *
     * @return 学习进度记录 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_learning_progress");
    }
}
