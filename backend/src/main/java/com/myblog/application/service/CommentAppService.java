package com.myblog.application.service;

import com.myblog.application.assembler.CommentAssembler;
import com.myblog.application.command.CreateCommentCommand;
import com.myblog.application.dto.CommentDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class CommentAppService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    /**
     * 创建评论应用服务。
     *
     * @param commentRepository 评论仓储
     * @param articleRepository 文章仓储
     * @param userRepository 用户仓储
     */
    public CommentAppService(CommentRepository commentRepository, ArticleRepository articleRepository,
                            UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取文章评论树。
     *
     * @param articleId 文章 ID
     * @return 评论树
     */
    public List<CommentDTO> getArticleComments(Long articleId) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        List<Comment> comments = commentRepository.findByArticleId(article.getId());

        List<Long> userIds = comments.stream()
            .map(c -> c.getUserId().getValue())
            .distinct()
            .collect(Collectors.toList());

        Map<Long, User> userMap = userIds.stream()
            .map(id -> userRepository.findById(new UserId(id)).orElse(null))
            .filter(u -> u != null)
            .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u));

        List<CommentDTO> allDTOs = new ArrayList<>();
        for (Comment comment : comments) {
            User user = userMap.get(comment.getUserId().getValue());
            allDTOs.add(CommentAssembler.toDTO(comment, user));
        }

        return buildCommentTree(allDTOs);
    }

    /**
     * 创建评论。
     *
     * @param command 创建评论命令
     * @return 评论数据
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO createComment(CreateCommentCommand command) {
        Article article = articleRepository.findById(new ArticleId(command.getArticleId()))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "不能在未发布的文章下评论");
        }

        User user = userRepository.findById(new UserId(command.getUserId()))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        if (!UserStatus.NORMAL.equals(user.getStatus())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "用户状态异常，不能评论");
        }

        if (command.getParentId() != null && command.getParentId() > 0) {
            Comment parentComment = commentRepository.findById(new CommentId(command.getParentId()))
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "父评论不存在"));

            if (!parentComment.getArticleId().getValue().equals(command.getArticleId())) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "父评论不属于该文章");
            }
        }

        Comment comment = Comment.create(
            commentRepository.nextId(),
            article.getId(),
            user.getId(),
            command.getParentId(),
            command.getContent()
        );

        commentRepository.save(comment);

        article.increaseCommentCount();
        articleRepository.save(article);

        return CommentAssembler.toDTO(comment, user);
    }

    /**
     * 删除评论。
     *
     * @param commentId 评论 ID
     * @param userId 操作用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));

        if (comment.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "评论已删除");
        }

        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        boolean isCommentAuthor = comment.getUserId().getValue().equals(userId);
        boolean isArticleAuthor = comment.getArticleId().getValue() != null &&
            articleRepository.findById(comment.getArticleId())
                .map(a -> a.getAuthorId().getValue().equals(userId))
                .orElse(false);
        boolean isAdmin = UserRole.ADMIN.equals(user.getRole());

        if (!isCommentAuthor && !isArticleAuthor && !isAdmin) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权限删除该评论");
        }

        List<Comment> articleComments = commentRepository.findByArticleId(comment.getArticleId());
        List<Comment> commentsToDelete = collectCommentsToDelete(comment, articleComments);
        for (Comment item : commentsToDelete) {
            item.delete();
            commentRepository.save(item);
        }

        Article article = articleRepository.findById(comment.getArticleId()).orElse(null);
        if (article != null) {
            for (int i = 0; i < commentsToDelete.size(); i++) {
                article.decreaseCommentCount();
            }
            articleRepository.save(article);
        }
    }

    private List<Comment> collectCommentsToDelete(Comment target, List<Comment> articleComments) {
        List<Comment> result = new ArrayList<>();
        result.add(target);
        collectChildren(target.getId().getValue(), articleComments, result);
        return result;
    }

    private void collectChildren(Long parentId, List<Comment> articleComments, List<Comment> result) {
        for (Comment comment : articleComments) {
            if (!parentId.equals(comment.getParentId())) {
                continue;
            }
            result.add(comment);
            collectChildren(comment.getId().getValue(), articleComments, result);
        }
    }

    private List<CommentDTO> buildCommentTree(List<CommentDTO> comments) {
        Map<Long, List<CommentDTO>> childrenMap = comments.stream()
            .filter(c -> c.getParentId() != null && c.getParentId() > 0)
            .collect(Collectors.groupingBy(CommentDTO::getParentId));

        List<CommentDTO> rootComments = comments.stream()
            .filter(c -> c.getParentId() == null || c.getParentId() == 0)
            .collect(Collectors.toList());

        for (CommentDTO comment : rootComments) {
            assignReplies(comment, childrenMap);
        }

        return rootComments;
    }

    private void assignReplies(CommentDTO comment, Map<Long, List<CommentDTO>> childrenMap) {
        List<CommentDTO> children = childrenMap.get(comment.getId());
        if (children != null && !children.isEmpty()) {
            for (CommentDTO child : children) {
                assignReplies(child, childrenMap);
            }
            comment.setReplies(children);
        } else {
            comment.setReplies(new ArrayList<>());
        }
    }
}
