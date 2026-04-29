package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.TopicDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专题 Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface TopicMapper {

    TopicDO selectById(@Param("id") Long id);

    List<TopicDO> selectPublished(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPublished();

    Long selectNextId();

    int countById(@Param("id") Long id);

    int insert(TopicDO topicDO);
    int insertOrUpdate(TopicDO topicDO);

    int update(TopicDO topicDO);

    List<Long> selectArticleIds(@Param("topicId") Long topicId);

    int countTopicArticle(@Param("topicId") Long topicId, @Param("articleId") Long articleId);

    int insertTopicArticle(@Param("topicId") Long topicId, @Param("articleId") Long articleId,
                           @Param("sortOrder") int sortOrder);

    int deleteTopicArticle(@Param("topicId") Long topicId, @Param("articleId") Long articleId);

    int incrementArticleCount(@Param("topicId") Long topicId);

    int decrementArticleCount(@Param("topicId") Long topicId);

    /** 后台管理：分页查询所有专题 */
    List<TopicDO> selectAll(@Param("keyword") String keyword,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    long countAll(@Param("keyword") String keyword);

    /** 后台管理：软删除专题 */
    int softDelete(@Param("id") Long id);
}
