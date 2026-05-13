package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.TopicDO;
import com.myblog.infrastructure.repository.persistence.entity.LearningPathArticleDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专题 Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface TopicMapper {

    /**
     * 根据 ID 查询专题。
     *
     * @param id 专题 ID
     * @return 专题数据对象
     */
    TopicDO selectById(@Param("id") Long id);

    /**
     * 分页查询已发布专题。
     *
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 专题列表
     */
    List<TopicDO> selectPublished(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计已发布专题数量。
     *
     * @return 已发布专题数量
     */
    long countPublished();

    /**
     * 查询下一个专题 ID。
     *
     * @return 下一个专题 ID
     */
    Long selectNextId();

    /**
     * 根据 ID 统计专题数量。
     *
     * @param id 专题 ID
     * @return 专题数量
     */
    int countById(@Param("id") Long id);

    /**
     * 插入专题。
     *
     * @param topicDO 专题数据对象
     * @return 影响行数
     */
    int insert(TopicDO topicDO);

    /**
     * 插入或更新专题（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param topicDO 专题数据对象
     * @return 影响行数
     */
    int insertOrUpdate(TopicDO topicDO);

    /**
     * 更新专题。
     *
     * @param topicDO 专题数据对象
     * @return 影响行数
     */
    int update(TopicDO topicDO);

    /**
     * 查询专题下的文章 ID 列表。
     *
     * @param topicId 专题 ID
     * @return 文章 ID 列表
     */
    List<Long> selectArticleIds(@Param("topicId") Long topicId);

    /**
     * 查询专题下文章关联关系（含学习路径信息）。
     *
     * @param topicId 专题 ID
     * @return 学习路径文章列表
     */
    List<LearningPathArticleDO> selectArticleRelations(@Param("topicId") Long topicId);

    /**
     * 搜索已发布专题。
     *
     * @param keyword    关键字
     * @param difficulty 难度
     * @param offset     偏移量
     * @param limit      限制数量
     * @return 专题列表
     */
    List<TopicDO> searchPublished(@Param("keyword") String keyword,
                                  @Param("difficulty") String difficulty,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /**
     * 统计已发布专题搜索数量。
     *
     * @param keyword    关键字
     * @param difficulty 难度
     * @return 专题数量
     */
    long countSearchPublished(@Param("keyword") String keyword, @Param("difficulty") String difficulty);

    /**
     * 统计专题文章关联数量。
     *
     * @param topicId   专题 ID
     * @param articleId 文章 ID
     * @return 关联数量
     */
    int countTopicArticle(@Param("topicId") Long topicId, @Param("articleId") Long articleId);

    /**
     * 新增专题文章关联。
     *
     * @param topicId   专题 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序
     * @return 影响行数
     */
    int insertTopicArticle(@Param("topicId") Long topicId, @Param("articleId") Long articleId,
                           @Param("sortOrder") int sortOrder);

    /**
     * 删除专题文章关联。
     *
     * @param topicId   专题 ID
     * @param articleId 文章 ID
     * @return 影响行数
     */
    int deleteTopicArticle(@Param("topicId") Long topicId, @Param("articleId") Long articleId);

    /**
     * 原子递增专题文章数。
     *
     * @param topicId 专题 ID
     * @return 影响行数
     */
    int incrementArticleCount(@Param("topicId") Long topicId);

    /**
     * 原子递减专题文章数（防止降为负数）。
     *
     * @param topicId 专题 ID
     * @return 影响行数
     */
    int decrementArticleCount(@Param("topicId") Long topicId);

    /**
     * 后台管理：分页查询所有专题。
     *
     * @param keyword 关键字
     * @param offset  偏移量
     * @param limit   限制数量
     * @return 专题列表
     */
    List<TopicDO> selectAll(@Param("keyword") String keyword,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    /**
     * 统计所有专题数量。
     *
     * @param keyword 关键字
     * @return 专题数量
     */
    long countAll(@Param("keyword") String keyword);

    /**
     * 后台管理：软删除专题。
     *
     * @param id 专题 ID
     * @return 影响行数
     */
    int softDelete(@Param("id") Long id);

    /**
     * 按近期活跃度查询热门专题。
     * 统计指定天数内专题下文章的互动热度分（view_count + like_count*3 + comment_count*5），取前 limit 条。
     *
     * @param withinDays 统计天数
     * @param limit      返回数量
     * @return 热门专题列表
     */
    List<TopicDO> selectHotByActivity(@Param("withinDays") int withinDays, @Param("limit") int limit);
}
