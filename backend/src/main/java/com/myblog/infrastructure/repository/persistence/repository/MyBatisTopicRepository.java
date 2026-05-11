package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.model.valueobject.LearningPathArticle;
import com.myblog.domain.model.valueobject.TopicId;
import com.myblog.domain.repository.TopicRepository;
import com.myblog.infrastructure.repository.persistence.converter.TopicPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.LearningPathArticleDO;
import com.myblog.infrastructure.repository.persistence.entity.TopicDO;
import com.myblog.infrastructure.repository.persistence.mapper.TopicMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 专题 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisTopicRepository implements TopicRepository {

    private final TopicMapper topicMapper;

    public MyBatisTopicRepository(TopicMapper topicMapper) {
        this.topicMapper = topicMapper;
    }

    @Override
    public Optional<Topic> findById(TopicId topicId) {
        TopicDO topicDO = topicMapper.selectById(topicId.getValue());
        return topicDO == null ? Optional.empty() : Optional.of(toDomain(topicDO));
    }

    @Override
    public List<Topic> findPublished(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<TopicDO> topicDOList = topicMapper.selectPublished((currentPage - 1) * currentPageSize, currentPageSize);
        List<Topic> topics = new ArrayList<>(topicDOList.size());
        for (TopicDO topicDO : topicDOList) {
            topics.add(toDomain(topicDO));
        }
        return topics;
    }

    @Override
    public long countPublished() {
        return topicMapper.countPublished();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Topic save(Topic topic) {
        TopicDO topicDO = toData(topic);
        topicMapper.insertOrUpdate(topicDO);
        return topic;
    }

    @Override
    public Long nextId() {
        return topicMapper.selectNextId();
    }

    @Override
    public List<Long> findArticleIds(TopicId topicId) {
        return topicMapper.selectArticleIds(topicId.getValue());
    }

    @Override
    public List<LearningPathArticle> findArticleRelations(TopicId topicId) {
        List<LearningPathArticleDO> list = topicMapper.selectArticleRelations(topicId.getValue());
        List<LearningPathArticle> result = new ArrayList<>(list.size());
        for (LearningPathArticleDO item : list) {
            result.add(toPathArticle(item));
        }
        return result;
    }

    @Override
    public List<Topic> searchPublished(String keyword, String difficulty, int page, int pageSize) {
        int p = Math.max(page, 1);
        int ps = Math.max(pageSize, 1);
        List<TopicDO> list = topicMapper.searchPublished(keyword, difficulty, (p - 1) * ps, ps);
        List<Topic> topics = new ArrayList<>(list.size());
        for (TopicDO topicDO : list) {
            topics.add(toDomain(topicDO));
        }
        return topics;
    }

    @Override
    public long countSearchPublished(String keyword, String difficulty) {
        return topicMapper.countSearchPublished(keyword, difficulty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindArticle(TopicId topicId, Long articleId, int sortOrder) {
        if (topicMapper.countTopicArticle(topicId.getValue(), articleId) > 0) {
            return;
        }
        topicMapper.insertTopicArticle(topicId.getValue(), articleId, sortOrder);
        topicMapper.incrementArticleCount(topicId.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindArticle(TopicId topicId, Long articleId) {
        topicMapper.deleteTopicArticle(topicId.getValue(), articleId);
        topicMapper.decrementArticleCount(topicId.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementArticleCount(TopicId topicId) {
        topicMapper.incrementArticleCount(topicId.getValue());
    }

    @Override
    public List<Topic> findAll(String keyword, int page, int pageSize) {
        int p = Math.max(page, 1);
        int ps = Math.max(pageSize, 1);
        List<TopicDO> list = topicMapper.selectAll(keyword, (p - 1) * ps, ps);
        List<Topic> topics = new ArrayList<>(list.size());
        for (TopicDO d : list) {
            topics.add(toDomain(d));
        }
        return topics;
    }

    @Override
    public long countAll(String keyword) {
        return topicMapper.countAll(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(TopicId topicId) {
        topicMapper.softDelete(topicId.getValue());
    }

    @Override
    public List<Topic> findHotByActivity(int withinDays, int limit) {
        int days = Math.max(withinDays, 1);
        int lim = Math.max(limit, 1);
        List<TopicDO> list = topicMapper.selectHotByActivity(days, lim);
        List<Topic> topics = new ArrayList<>(list.size());
        for (TopicDO d : list) {
            topics.add(toDomain(d));
        }
        return topics;
    }

    private Topic toDomain(TopicDO topicDO) {
        return TopicPersistenceConverter.toDomain(topicDO);
    }

    private TopicDO toData(Topic topic) {
        TopicDO topicDO = TopicPersistenceConverter.toData(topic);
        if (topicDO.getCreatedAt() == null) {
            topicDO.setCreatedAt(LocalDateTime.now());
        }
        topicDO.setUpdatedAt(LocalDateTime.now());
        if (topicDO.getVersion() == null) {
            topicDO.setVersion(0);
        }
        return topicDO;
    }

    private LearningPathArticle toPathArticle(LearningPathArticleDO item) {
        return new LearningPathArticle(
            item.getArticleId(),
            item.getSectionTitle(),
            item.getStepOrder() == null ? 0 : item.getStepOrder(),
            item.getRequired() == null || item.getRequired(),
            item.getEditorNote()
        );
    }
}
