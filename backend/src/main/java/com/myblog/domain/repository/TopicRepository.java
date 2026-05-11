package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.model.valueobject.LearningPathArticle;
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

    List<LearningPathArticle> findArticleRelations(TopicId topicId);

    List<Topic> searchPublished(String keyword, String difficulty, int page, int pageSize);

    long countSearchPublished(String keyword, String difficulty);

    void bindArticle(TopicId topicId, Long articleId, int sortOrder);

    void unbindArticle(TopicId topicId, Long articleId);

    void incrementArticleCount(TopicId topicId);

    /** 后台管理：分页查询所有专题（含下架，不含删除） */
    List<Topic> findAll(String keyword, int page, int pageSize);

    long countAll(String keyword);

    /** 后台管理：软删除专题 */
    void softDelete(TopicId topicId);

    /**
     * 按近期活跃度查询热门专题：统计 N 天内专题下文章的互动热度，取前 limit 条。
     *
     * @param withinDays 统计天数（如 7）
     * @param limit      返回数量
     * @return 按热度降序的专题列表
     */
    List<Topic> findHotByActivity(int withinDays, int limit);
}
