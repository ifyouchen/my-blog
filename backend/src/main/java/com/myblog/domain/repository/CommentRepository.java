package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;

import java.util.List;
import java.util.Optional;

/**
 * 评论仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CommentRepository {

    /**
     * 根据评论 ID 查询评论。
     *
     * @param commentId 评论 ID
     * @return 评论 Optional
     */
    Optional<Comment> findById(CommentId commentId);

    /**
     * 根据文章 ID 查询评论列表。
     *
     * @param articleId 文章 ID
     * @return 评论列表
     */
    List<Comment> findByArticleId(ArticleId articleId);

    /**
     * 保存评论。
     *
     * @param comment 评论聚合根
     * @return 保存后的评论
     */
    Comment save(Comment comment);

    /**
     * 查询所有评论。
     *
     * @return 评论列表
     */
    List<Comment> findAll();

    /**
     * 生成下一个评论 ID。
     *
     * @return 评论 ID
     */
    Long nextId();
}
