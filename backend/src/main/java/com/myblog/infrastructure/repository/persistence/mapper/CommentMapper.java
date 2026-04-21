package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.CommentDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CommentMapper {

    /**
     * 根据 ID 查询评论。
     *
     * @param id 评论 ID
     * @return 评论数据对象
     */
    CommentDO selectById(@Param("id") Long id);

    /**
     * 查询所有评论。
     *
     * @return 评论数据对象列表
     */
    List<CommentDO> selectAll();

    /**
     * 根据文章 ID 查询评论列表。
     *
     * @param articleId 文章 ID
     * @return 评论数据对象列表
     */
    List<CommentDO> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据 ID 统计评论数量。
     *
     * @param id 评论 ID
     * @return 评论数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个评论 ID。
     *
     * @return 下一个评论 ID
     */
    Long selectNextId();

    /**
     * 插入评论。
     *
     * @param commentDO 评论数据对象
     * @return 影响行数
     */
    int insert(CommentDO commentDO);

    /**
     * 更新评论。
     *
     * @param commentDO 评论数据对象
     * @return 影响行数
     */
    int update(CommentDO commentDO);

    /**
     * 逻辑删除评论。
     *
     * @param id 评论 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
