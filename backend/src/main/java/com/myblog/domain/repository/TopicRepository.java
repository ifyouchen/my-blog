package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.model.valueobject.LearningPathArticle;
import com.myblog.domain.model.valueobject.TopicId;

import java.util.List;
import java.util.Optional;

/**
 * 专题仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface TopicRepository {

    /**
     * 根据专题 ID 查询专题。
     *
     * @param topicId 专题 ID
     * @return 专题 Optional
     */
    Optional<Topic> findById(TopicId topicId);

    /**
     * 分页查询已发布专题。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专题列表
     */
    List<Topic> findPublished(int page, int pageSize);

    /**
     * 统计已发布专题数量。
     *
     * @return 已发布专题数量
     */
    long countPublished();

    /**
     * 保存专题（新增或更新）。
     *
     * @param topic 专题聚合根
     * @return 保存后的专题
     */
    Topic save(Topic topic);

    /**
     * 生成下一个专题 ID。
     *
     * @return 专题 ID
     */
    Long nextId();

    /**
     * 查询专题关联的文章 ID 列表。
     *
     * @param topicId 专题 ID
     * @return 文章 ID 列表
     */
    List<Long> findArticleIds(TopicId topicId);

    /**
     * 查询专题的文章编排关系列表。
     *
     * @param topicId 专题 ID
     * @return 学习路径文章列表
     */
    List<LearningPathArticle> findArticleRelations(TopicId topicId);

    /**
     * 搜索已发布专题。
     *
     * @param keyword   关键字
     * @param difficulty 难度筛选
     * @param page      页码
     * @param pageSize  每页大小
     * @return 专题列表
     */
    List<Topic> searchPublished(String keyword, String difficulty, int page, int pageSize);

    /**
     * 统计搜索已发布专题数量。
     *
     * @param keyword   关键字
     * @param difficulty 难度筛选
     * @return 专题数量
     */
    long countSearchPublished(String keyword, String difficulty);

    /**
     * 绑定文章到专题。
     *
     * @param topicId   专题 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序权重
     */
    void bindArticle(TopicId topicId, Long articleId, int sortOrder);

    /**
     * 从专题解绑文章。
     *
     * @param topicId   专题 ID
     * @param articleId 文章 ID
     */
    void unbindArticle(TopicId topicId, Long articleId);

    /**
     * 原子递增专题文章数量。
     *
     * @param topicId 专题 ID
     */
    void incrementArticleCount(TopicId topicId);

    /**
     * 后台管理：分页查询所有专题（含下架，不含删除）。
     *
     * @param keyword  关键字
     * @param page     页码
     * @param pageSize 每页大小
     * @return 专题列表
     */
    List<Topic> findAll(String keyword, int page, int pageSize);

    long countAll(String keyword);

    /**
     * 后台管理：软删除专题。
     *
     * @param topicId 专题 ID
     */
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
