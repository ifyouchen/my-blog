package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Topic;
import com.myblog.infrastructure.repository.persistence.entity.TopicDO;

/**
 * 专题持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public class TopicPersistenceConverter {

    /**
     * 将专题数据对象转换为领域对象。
     *
     * @param topicDO 专题数据对象
     * @return 专题领域对象，若 topicDO 为 null 则返回 null
     */
    public static Topic toDomain(TopicDO topicDO) {
        if (topicDO == null) {
            return null;
        }
        return Topic.restore(
            topicDO.getId(),
            topicDO.getTitle(),
            topicDO.getSummary(),
            topicDO.getCoverUrl(),
            topicDO.getStatus(),
            topicDO.getSortOrder(),
            topicDO.getArticleCount(),
            topicDO.getIntro(),
            topicDO.getDifficulty(),
            topicDO.getEstimatedMinutes(),
            topicDO.getSourceType(),
            topicDO.getSourceNote(),
            topicDO.getRecommended(),
            topicDO.getRecommendWeight(),
            topicDO.getCreatedAt(),
            topicDO.getUpdatedAt(),
            topicDO.getDeletedAt(),
            topicDO.getVersion()
        );
    }

    /**
     * 将专题领域对象转换为数据对象。
     *
     * @param topic 专题领域对象
     * @return 专题数据对象，若 topic 为 null 则返回 null
     */
    public static TopicDO toData(Topic topic) {
        if (topic == null) {
            return null;
        }
        TopicDO topicDO = new TopicDO();
        topicDO.setId(topic.getId().getValue());
        topicDO.setTitle(topic.getTitle());
        topicDO.setSummary(topic.getSummary());
        topicDO.setCoverUrl(topic.getCoverUrl());
        topicDO.setStatus(topic.getStatus());
        topicDO.setSortOrder(topic.getSortOrder());
        topicDO.setArticleCount(topic.getArticleCount());
        topicDO.setIntro(topic.getIntro());
        topicDO.setDifficulty(topic.getDifficulty());
        topicDO.setEstimatedMinutes(topic.getEstimatedMinutes());
        topicDO.setSourceType(topic.getSourceType());
        topicDO.setSourceNote(topic.getSourceNote());
        topicDO.setRecommended(topic.isRecommended());
        topicDO.setRecommendWeight(topic.getRecommendWeight());
        topicDO.setCreatedAt(topic.getCreatedAt());
        topicDO.setUpdatedAt(topic.getUpdatedAt());
        topicDO.setDeletedAt(topic.getDeletedAt());
        topicDO.setVersion(topic.getVersion());
        return topicDO;
    }
}
