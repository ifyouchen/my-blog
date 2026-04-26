package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.infrastructure.repository.persistence.converter.CommentPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.CommentDO;
import com.myblog.infrastructure.repository.persistence.mapper.CommentMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 评论 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisCommentRepository implements CommentRepository {

    private final CommentMapper commentMapper;

    /**
     * 创建评论 MyBatis 仓储。
     *
     * @param commentMapper 评论 Mapper
     */
    public MyBatisCommentRepository(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    /**
     * 根据评论 ID 查询评论。
     *
     * @param commentId 评论 ID
     * @return 评论 Optional
     */
    @Override
    public Optional<Comment> findById(CommentId commentId) {
        CommentDO commentDO = commentMapper.selectById(commentId.getValue());
        if (commentDO == null) {
            return Optional.empty();
        }
        return Optional.of(CommentPersistenceConverter.toDomain(commentDO));
    }

    /**
     * 根据文章 ID 查询评论列表。
     *
     * @param articleId 文章 ID
     * @return 评论列表
     */
    @Override
    public List<Comment> findByArticleId(ArticleId articleId) {
        List<CommentDO> commentDOList = commentMapper.selectByArticleId(articleId.getValue());
        List<Comment> comments = new ArrayList<>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    @Override
    public List<Comment> findAdminPage(Long articleId, String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<CommentDO> commentDOList = commentMapper.selectAdminPage(articleId, keyword, offset, currentPageSize);
        List<Comment> comments = new ArrayList<Comment>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    @Override
    public long countAdminPage(Long articleId, String keyword) {
        return commentMapper.countAdminPage(articleId, keyword);
    }

    @Override
    public List<Comment> findRootComments(ArticleId articleId, String sort, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<CommentDO> commentDOList = commentMapper.selectRootPage(articleId.getValue(), sort, offset, currentPageSize);
        List<Comment> comments = new ArrayList<>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    @Override
    public long countRootComments(ArticleId articleId) {
        return commentMapper.countRootByArticleId(articleId.getValue());
    }

    @Override
    public List<Comment> findReplies(CommentId rootCommentId, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<CommentDO> commentDOList = commentMapper.selectRepliesByRootCommentId(
            rootCommentId.getValue(),
            offset,
            currentPageSize
        );
        List<Comment> comments = new ArrayList<>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    @Override
    public long countReplies(CommentId rootCommentId) {
        return commentMapper.countRepliesByRootCommentId(rootCommentId.getValue());
    }

    @Override
    public List<Comment> findReplyPreviewByRootIds(List<Long> rootCommentIds, int limitPerRoot) {
        if (rootCommentIds == null || rootCommentIds.isEmpty()) {
            return new ArrayList<Comment>();
        }
        List<CommentDO> commentDOList = commentMapper.selectReplyPreviewByRootIds(rootCommentIds, limitPerRoot);
        List<Comment> comments = new ArrayList<>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    @Override
    public List<Comment> findByIds(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return new ArrayList<Comment>();
        }
        List<CommentDO> commentDOList = commentMapper.selectByIds(commentIds);
        List<Comment> comments = new ArrayList<>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    @Override
    public List<Comment> findThreadByRootCommentId(CommentId rootCommentId) {
        List<CommentDO> commentDOList = commentMapper.selectThreadByRootCommentId(rootCommentId.getValue());
        List<Comment> comments = new ArrayList<>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    /**
     * 保存评论。
     *
     * @param comment 评论聚合根
     * @return 保存后的评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Comment save(Comment comment) {
        if (comment.isDeleted()) {
            commentMapper.deleteById(comment.getId().getValue());
            return comment;
        }
        CommentDO commentDO = CommentPersistenceConverter.toData(comment);
        if (commentMapper.countById(comment.getId().getValue()) > 0) {
            commentMapper.update(commentDO);
        } else {
            commentMapper.insert(commentDO);
        }
        return comment;
    }

    @Override
    public void clearPinnedByArticleId(ArticleId articleId) {
        commentMapper.clearPinnedByArticleId(articleId.getValue());
    }

    @Override
    public List<Comment> findAll() {
        List<CommentDO> commentDOList = commentMapper.selectAll();
        List<Comment> comments = new ArrayList<>(commentDOList.size());
        for (CommentDO commentDO : commentDOList) {
            comments.add(CommentPersistenceConverter.toDomain(commentDO));
        }
        return comments;
    }

    @Override
    public long countAll() {
        return commentMapper.countAll();
    }

    @Override
    public long countCreatedOn(LocalDate date) {
        return commentMapper.countCreatedOn(date);
    }

    @Override
    public long countCreatedSince(LocalDate date) {
        return commentMapper.countCreatedSince(date);
    }

    /**
     * 生成下一个评论 ID。
     *
     * @return 评论 ID
     */
    @Override
    public Long nextId() {
        Long nextId = commentMapper.selectNextId();
        return nextId == null ? 101L : nextId;
    }
}
