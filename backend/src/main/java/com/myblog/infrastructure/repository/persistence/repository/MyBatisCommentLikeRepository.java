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
    private final IdGenerator idGenerator;

    /**
     * 创建评论点赞 MyBatis 仓储。
     *
     * @param commentLikeMapper 评论点赞 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisCommentLikeRepository(CommentLikeMapper commentLikeMapper, IdGenerator idGenerator) {
        this.commentLikeMapper = commentLikeMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 查询有效的评论点赞记录（未取消）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 评论点赞 Optional
     */
    @Override
    public Optional<CommentLike> findByCommentAndUser(CommentId commentId, UserId userId) {
        CommentLikeDO commentLikeDO = commentLikeMapper.selectByCommentAndUser(commentId.getValue(), userId.getValue());
        return Optional.ofNullable(CommentLikePersistenceConverter.toDomain(commentLikeDO));
    }

    /**
     * 查询评论点赞记录（包含已逻辑删除的记录）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 评论点赞 Optional
     */
    @Override
    public Optional<CommentLike> findAnyByCommentAndUser(CommentId commentId, UserId userId) {
        CommentLikeDO commentLikeDO = commentLikeMapper.selectAnyByCommentAndUser(commentId.getValue(), userId.getValue());
        return Optional.ofNullable(CommentLikePersistenceConverter.toDomain(commentLikeDO));
    }

    /**
     * 判断用户是否已点赞评论。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 是否已点赞
     */
    @Override
    public boolean exists(CommentId commentId, UserId userId) {
        return commentLikeMapper.countByCommentAndUser(commentId.getValue(), userId.getValue()) > 0;
    }

    /**
     * 批量查询用户对多条评论的点赞记录。
     *
     * @param commentIds 评论 ID 列表
     * @param userId     用户 ID
     * @return 评论点赞列表
     */
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

    /**
     * 保存评论点赞记录。
     *
     * @param commentLike 评论点赞聚合根
     * @return 保存后的评论点赞聚合根
     */
    @Override
    public CommentLike save(CommentLike commentLike) {
        CommentLikeDO commentLikeDO = CommentLikePersistenceConverter.toData(commentLike);
        commentLikeMapper.insertOrUpdate(commentLikeDO);
        return commentLike;
    }

    /**
     * 生成下一个评论点赞记录 ID。
     *
     * @return 评论点赞记录 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_comment_like");
    }
}
