package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.CommentLike;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.CommentLikeRepository;
import com.myblog.infrastructure.repository.persistence.converter.CommentLikePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.CommentLikeDO;
import com.myblog.infrastructure.repository.persistence.mapper.CommentLikeMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
/**
 * 评论点赞 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisCommentLikeRepository implements CommentLikeRepository {

    private final CommentLikeMapper commentLikeMapper;

    public MyBatisCommentLikeRepository(CommentLikeMapper commentLikeMapper) {
        this.commentLikeMapper = commentLikeMapper;
    }

    @Override
    public Optional<CommentLike> findByCommentAndUser(CommentId commentId, UserId userId) {
        CommentLikeDO commentLikeDO = commentLikeMapper.selectByCommentAndUser(commentId.getValue(), userId.getValue());
        return Optional.ofNullable(CommentLikePersistenceConverter.toDomain(commentLikeDO));
    }

    @Override
    public Optional<CommentLike> findAnyByCommentAndUser(CommentId commentId, UserId userId) {
        CommentLikeDO commentLikeDO = commentLikeMapper.selectAnyByCommentAndUser(commentId.getValue(), userId.getValue());
        return Optional.ofNullable(CommentLikePersistenceConverter.toDomain(commentLikeDO));
    }

    @Override
    public boolean exists(CommentId commentId, UserId userId) {
        return commentLikeMapper.countByCommentAndUser(commentId.getValue(), userId.getValue()) > 0;
    }

    @Override
    public List<CommentLike> findByCommentIdsAndUser(List<Long> commentIds, UserId userId) {
        if (commentIds == null || commentIds.isEmpty()) {
            return new ArrayList<CommentLike>();
        }
        List<CommentLikeDO> commentLikeDOList = commentLikeMapper.selectByCommentIdsAndUser(commentIds, userId.getValue());
        List<CommentLike> commentLikes = new ArrayList<CommentLike>(commentLikeDOList.size());
        for (CommentLikeDO commentLikeDO : commentLikeDOList) {
            commentLikes.add(CommentLikePersistenceConverter.toDomain(commentLikeDO));
        }
        return commentLikes;
    }

    @Override
    public CommentLike save(CommentLike commentLike) {
        CommentLikeDO commentLikeDO = CommentLikePersistenceConverter.toData(commentLike);
        commentLikeMapper.insertOrUpdate(commentLikeDO);
        return commentLike;
    }

    @Override
    public Long nextId() {
        return commentLikeMapper.selectNextId();
    }
}
