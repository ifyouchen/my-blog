package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.model.valueobject.TopicId;

import java.util.List;
import java.util.Optional;

/**
 * 专题仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface TopicRepository {

    Optional<Topic> findById(TopicId topicId);

    List<Topic> findPublished(int page, int pageSize);

    long countPublished();

    Topic save(Topic topic);

    Long nextId();

    List<Long> findArticleIds(TopicId topicId);

    void bindArticle(TopicId topicId, Long articleId, int sortOrder);

    void unbindArticle(TopicId topicId, Long articleId);

    void incrementArticleCount(TopicId topicId);

    /** 后台管理：分页查询所有专题（含下架，不含删除） */
    List<Topic> findAll(String keyword, int page, int pageSize);

    long countAll(String keyword);

    /** 后台管理：软删除专题 */
    void softDelete(TopicId topicId);
}
