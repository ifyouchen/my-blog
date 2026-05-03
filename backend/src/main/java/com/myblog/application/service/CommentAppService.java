package com.myblog.application.service;

import com.myblog.application.assembler.CommentAssembler;
import com.myblog.application.command.CreateCommentCommand;
import com.myblog.application.dto.CommentDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.event.CommentCreatedEvent;
import com.myblog.application.event.CommentDeletedEvent;
import com.myblog.application.event.CommentLikedEvent;
import com.myblog.application.event.CommentUnlikedEvent;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.CommentLike;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentLikeRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 评论应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class CommentAppService {

    private static final Logger log = LoggerFactory.getLogger(CommentAppService.class);
    private static final int DEFAULT_REPLY_PREVIEW_SIZE = 3;

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final SensitiveWordAppService sensitiveWordAppService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建评论应用服务。
     *
     * @param commentRepository 评论仓储
     * @param commentLikeRepository 评论点赞仓储
     * @param articleRepository 文章仓储
     * @param userRepository 用户仓储
     */
    public CommentAppService(CommentRepository commentRepository,
                             CommentLikeRepository commentLikeRepository,
                             ArticleRepository articleRepository,
                             UserRepository userRepository,
                             SensitiveWordAppService sensitiveWordAppService,
                             ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.sensitiveWordAppService = sensitiveWordAppService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 分页查询文章一级评论。
     *
     * @param articleId 文章 ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param sort 排序方式
     * @param currentUserId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 评论分页结果
     */
    public PageResult<CommentDTO> pageArticleComments(Long articleId, int page, int pageSize,
                                                      String sort, Long currentUserId, String currentUserRole) {
        Article article = loadAccessibleArticle(articleId, currentUserId, currentUserRole);
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        String resolvedSort = normalizeSort(sort);

        List<Comment> rootComments = commentRepository.findRootComments(
            article.getId(),
            resolvedSort,
            currentPage,
            currentPageSize
        );
        long total = commentRepository.countRootComments(article.getId());
        List<Long> rootCommentIds = toCommentIds(rootComments);
        Map<Long, Integer> replyCountMap = loadReplyCounts(rootCommentIds);
        List<Comment> replyPreviewComments = commentRepository.findReplyPreviewByRootIds(
            rootCommentIds,
            DEFAULT_REPLY_PREVIEW_SIZE
        );
        Map<Long, List<Comment>> replyPreviewMap = groupByRootCommentId(replyPreviewComments);
        Map<Long, Comment> parentCommentMap = loadParentComments(replyPreviewComments);
        Map<Long, User> userMap = loadUsers(rootComments, replyPreviewComments, parentCommentMap.values());
        Set<Long> likedCommentIds = loadLikedCommentIds(rootComments, replyPreviewComments, currentUserId);

        List<CommentDTO> items = new ArrayList<CommentDTO>(rootComments.size());
        for (Comment rootComment : rootComments) {
            items.add(buildCommentDTO(
                rootComment,
                article,
                userMap,
                parentCommentMap,
                likedCommentIds,
                replyCountMap.get(rootComment.getId().getValue()),
                replyPreviewMap.get(rootComment.getId().getValue()),
                currentUserId,
                currentUserRole
            ));
        }
        return new PageResult<CommentDTO>(items, currentPage, currentPageSize, total);
    }

    /**
     * 分页查询楼中楼回复。
     *
     * @param rootCommentId 根评论 ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param currentUserId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 回复分页结果
     */
    public PageResult<CommentDTO> pageReplies(Long rootCommentId, int page, int pageSize,
                                              Long currentUserId, String currentUserRole) {
        Comment rootComment = commentRepository.findById(new CommentId(rootCommentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "根评论不存在"));
        if (!rootComment.isRootComment()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "仅支持查看一级评论的楼中楼回复");
        }

        Article article = loadAccessibleArticle(rootComment.getArticleId().getValue(), currentUserId, currentUserRole);
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);

        List<Comment> replies = commentRepository.findReplies(new CommentId(rootCommentId), currentPage, currentPageSize);
        long total = commentRepository.countReplies(new CommentId(rootCommentId));
        Map<Long, Comment> parentCommentMap = loadParentComments(replies);
        Map<Long, User> userMap = loadUsers(Collections.<Comment>emptyList(), replies, parentCommentMap.values());
        Set<Long> likedCommentIds = loadLikedCommentIds(Collections.<Comment>emptyList(), replies, currentUserId);

        List<CommentDTO> items = new ArrayList<CommentDTO>(replies.size());
        for (Comment reply : replies) {
            items.add(buildCommentDTO(
                reply,
                article,
                userMap,
                parentCommentMap,
                likedCommentIds,
                0,
                Collections.<Comment>emptyList(),
                currentUserId,
                currentUserRole
            ));
        }
        return new PageResult<CommentDTO>(items, currentPage, currentPageSize, total);
    }

    /**
     * 创建评论。
     *
     * @param command 创建评论命令
     * @param currentUserRole 当前用户角色
     * @return 评论数据
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO createComment(CreateCommentCommand command, String currentUserRole) {
        long _start = System.currentTimeMillis();
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

        Comment parentComment = null;
        Long rootCommentId = null;
        Long parentId = command.getParentId();
        if (parentId != null && parentId > 0) {
            parentComment = commentRepository.findById(new CommentId(parentId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "父评论不存在"));
            if (!parentComment.getArticleId().getValue().equals(command.getArticleId())) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "父评论不属于该文章");
            }
            rootCommentId = parentComment.isRootComment()
                ? parentComment.getId().getValue()
                : parentComment.getRootCommentId();
            if (command.getRootCommentId() != null
                && command.getRootCommentId() > 0
                && !command.getRootCommentId().equals(rootCommentId)) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "楼中楼根评论不匹配");
            }
        }

        SanitizedCommentContent sanitizedContent = sanitizeCommentContent(command.getContent(), "评论");

        Long nextCommentId = commentRepository.nextId();
        Long targetRootCommentId = rootCommentId != null ? rootCommentId : nextCommentId;
        Comment comment = Comment.create(
            nextCommentId,
            article.getId(),
            user.getId(),
            targetRootCommentId,
            parentId,
            sanitizedContent.getContent()
        );
        comment.approve();
        commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentCreatedEvent(
            comment.getId().getValue(),
            article.getId().getValue(),
            user.getId().getValue()
        ));

        CommentDTO dto = CommentAssembler.toDTO(comment, user);
        dto.setReplyCount(0);
        dto.setLiked(Boolean.FALSE);
        dto.setCanDelete(Boolean.TRUE);
        dto.setCanPin(Boolean.TRUE.equals(canPin(comment, article, command.getUserId(), currentUserRole)));
        dto.setCanEdit(canEdit(comment, command.getUserId()));
        dto.setAuthor(article.getAuthorId().getValue().equals(command.getUserId()));
        dto.setReplyPreview(new ArrayList<CommentDTO>());
        if (parentComment != null) {
            User replyToUser = userRepository.findById(parentComment.getUserId()).orElse(null);
            if (replyToUser != null) {
                dto.setReplyToUser(toUserDTO(replyToUser));
            }
        }
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(user.getId().getValue(), user.getNickname()),
            "创建评论",
            BizLogHelper.params("articleId", command.getArticleId(), "parentId", command.getParentId(),
                "content", BizLogHelper.contentMeta(sanitizedContent.getContent())),
            BizLogHelper.created("commentId", comment.getId().getValue()),
            BizLogHelper.elapsed(_start));
        return dto;
    }

    /**
     * 编辑评论内容（仅允许发布后 10 分钟内，且只有评论作者可操作）。
     *
     * @param commentId 评论 ID
     * @param newContent 新内容
     * @param userId 操作用户 ID
     * @return 更新后的评论数据
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO editComment(Long commentId, String newContent, Long userId) {
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        if (!comment.getUserId().getValue().equals(userId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "只能编辑自己的评论");
        }
        SanitizedCommentContent sanitizedContent = sanitizeCommentContent(newContent, "编辑评论");

        // 仅允许发布后 10 分钟内编辑
        java.time.LocalDateTime deadline = comment.getCreatedAt().plusMinutes(10);
        if (java.time.LocalDateTime.now().isAfter(deadline)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "评论发布超过 10 分钟，不可编辑");
        }
        comment.edit(sanitizedContent.getContent());
        commentRepository.save(comment);
        User user = userRepository.findById(comment.getUserId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        return CommentAssembler.toDTO(comment, user);
    }

    private SanitizedCommentContent sanitizeCommentContent(String content, String action) {
        List<String> blockHits = sensitiveWordAppService.detectBlockWords(content);
        if (!blockHits.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR,
                action + "失败：内容包含被禁止的敏感词（" + String.join("、", blockHits) + "），请修改后重试");
        }
        List<String> warnHits = sensitiveWordAppService.detectWarnWords(content);
        return new SanitizedCommentContent(
            sensitiveWordAppService.maskWarnWords(content),
            !warnHits.isEmpty()
        );
    }

    private static class SanitizedCommentContent {
        private final String content;
        private final boolean warnHits;

        SanitizedCommentContent(String content, boolean warnHits) {
            this.content = content;
            this.warnHits = warnHits;
        }

        String getContent() {
            return content;
        }

        boolean hasWarnHits() {
            return warnHits;
        }
    }

    /**
     * 删除评论。
     *
     * @param commentId 评论 ID
     * @param userId 操作用户 ID
     * @param currentUserRole 当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId, String currentUserRole) {
        long _start = System.currentTimeMillis();
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        Article article = articleRepository.findById(comment.getArticleId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!canDelete(comment, article, userId, currentUserRole)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权限删除该评论");
        }

        Long articleId = article.getId().getValue();
        int deleteCount;

        if (comment.isRootComment()) {
            comment.delete();
            commentRepository.save(comment);
            deleteCount = 1 + commentRepository.deleteThreadByRootCommentId(comment.getId());
        } else {
            comment.delete();
            commentRepository.save(comment);
            deleteCount = 1;
        }

        eventPublisher.publishEvent(new CommentDeletedEvent(commentId, articleId, deleteCount));
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "删除评论",
            BizLogHelper.params("commentId", commentId, "articleId", articleId),
            BizLogHelper.result("删除数量=" + deleteCount),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 点赞评论。
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId, Long userId) {
        long _start = System.currentTimeMillis();
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        UserId targetUserId = new UserId(userId);

        CommentLike existingLike = commentLikeRepository.findAnyByCommentAndUser(comment.getId(), targetUserId).orElse(null);
        if (existingLike != null) {
            if (!existingLike.isDeleted()) {
                throw new ApplicationException(ErrorCode.CONFLICT, "已经点赞过了");
            }
            existingLike.reactivate();
            commentLikeRepository.save(existingLike);
        } else {
            CommentLike commentLike = CommentLike.create(commentLikeRepository.nextId(), comment.getId(), targetUserId);
            commentLikeRepository.save(commentLike);
        }

        eventPublisher.publishEvent(new CommentLikedEvent(commentId, userId));
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "点赞评论",
            BizLogHelper.params("commentId", commentId),
            BizLogHelper.result("liked=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 取消评论点赞。
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId, Long userId) {
        long _start = System.currentTimeMillis();
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        CommentLike commentLike = commentLikeRepository.findAnyByCommentAndUser(comment.getId(), new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "点赞记录不存在"));
        if (commentLike.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消点赞了");
        }
        commentLike.delete();
        commentLikeRepository.save(commentLike);

        eventPublisher.publishEvent(new CommentUnlikedEvent(commentId, userId));
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "取消评论点赞",
            BizLogHelper.params("commentId", commentId),
            BizLogHelper.result("liked=false"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 置顶一级评论。
     *
     * @param commentId 评论 ID
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void pinComment(Long commentId, Long userId, String currentUserRole) {
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        Article article = articleRepository.findById(comment.getArticleId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!canPin(comment, article, userId, currentUserRole)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权限置顶该评论");
        }

        commentRepository.clearPinnedByArticleId(article.getId());
        comment.pin();
        commentRepository.save(comment);
    }

    /**
     * 取消置顶一级评论。
     *
     * @param commentId 评论 ID
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void unpinComment(Long commentId, Long userId, String currentUserRole) {
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        Article article = articleRepository.findById(comment.getArticleId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!canPin(comment, article, userId, currentUserRole)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权限取消置顶该评论");
        }
        if (!comment.isPinned()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "评论当前不是置顶状态");
        }
        comment.unpin();
        commentRepository.save(comment);
    }

    /**
     * 审核通过评论（管理员）。
     *
     * @param commentId 评论 ID
     * @return 审核后的评论
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO approveComment(Long commentId) {
        Comment comment = commentRepository.findByIdForAdmin(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        if (comment.isPublished()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "评论已发布，无需重复审核");
        }
        if (comment.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已删除的评论不能审核通过");
        }
        comment.approve();
        commentRepository.save(comment);
        User user = userRepository.findById(comment.getUserId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        return CommentAssembler.toDTO(comment, user);
    }

    /**
     * 拒绝评论（管理员，将评论标记为已删除）。
     *
     * @param commentId 评论 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectComment(Long commentId) {
        Comment comment = commentRepository.findByIdForAdmin(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
        if (comment.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "评论已删除");
        }
        comment.delete();
        commentRepository.save(comment);
    }

    private Article loadAccessibleArticle(Long articleId, Long currentUserId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            return article;
        }
        if (currentUserId != null && article.getAuthorId().getValue().equals(currentUserId)) {
            return article;
        }
        if (UserRole.ADMIN.name().equals(currentUserRole)) {
            return article;
        }
        throw new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在");
    }

    private String normalizeSort(String sort) {
        return "hot".equalsIgnoreCase(sort) ? "hot" : "latest";
    }

    private List<Long> toCommentIds(List<Comment> comments) {
        List<Long> commentIds = new ArrayList<Long>(comments.size());
        for (Comment comment : comments) {
            commentIds.add(comment.getId().getValue());
        }
        return commentIds;
    }

    private Map<Long, Integer> loadReplyCounts(List<Long> rootCommentIds) {
        if (rootCommentIds == null || rootCommentIds.isEmpty()) {
            return new HashMap<Long, Integer>();
        }
        // 批量查询回复数，解决 N+1 问题
        return commentRepository.countRepliesBatch(rootCommentIds);
    }

    private Map<Long, List<Comment>> groupByRootCommentId(List<Comment> comments) {
        Map<Long, List<Comment>> grouped = new HashMap<Long, List<Comment>>();
        for (Comment comment : comments) {
            Long rootCommentId = comment.getRootCommentId();
            List<Comment> items = grouped.get(rootCommentId);
            if (items == null) {
                items = new ArrayList<Comment>();
                grouped.put(rootCommentId, items);
            }
            items.add(comment);
        }
        return grouped;
    }

    private Map<Long, Comment> loadParentComments(List<Comment> comments) {
        Set<Long> parentIds = new HashSet<Long>();
        for (Comment comment : comments) {
            if (comment.getParentId() != null && comment.getParentId() > 0) {
                parentIds.add(comment.getParentId());
            }
        }
        if (parentIds.isEmpty()) {
            return new HashMap<Long, Comment>();
        }
        List<Comment> parentComments = commentRepository.findByIds(new ArrayList<Long>(parentIds));
        Map<Long, Comment> parentCommentMap = new HashMap<Long, Comment>();
        for (Comment parentComment : parentComments) {
            parentCommentMap.put(parentComment.getId().getValue(), parentComment);
        }
        return parentCommentMap;
    }

    private Map<Long, User> loadUsers(List<Comment> rootComments,
                                      List<Comment> replies,
                                      Iterable<Comment> parentComments) {
        Set<Long> userIds = new HashSet<Long>();
        collectUserIds(rootComments, userIds);
        collectUserIds(replies, userIds);
        if (parentComments != null) {
            for (Comment parentComment : parentComments) {
                if (parentComment != null) {
                    userIds.add(parentComment.getUserId().getValue());
                }
            }
        }
        if (userIds.isEmpty()) {
            return new HashMap<Long, User>();
        }
        // 批量查询用户，解决 N+1 问题
        List<User> users = userRepository.findByIds(new ArrayList<Long>(userIds));
        Map<Long, User> userMap = new HashMap<Long, User>(users.size() * 2);
        for (User user : users) {
            userMap.put(user.getId().getValue(), user);
        }
        return userMap;
    }

    private void collectUserIds(List<Comment> comments, Set<Long> userIds) {
        for (Comment comment : comments) {
            userIds.add(comment.getUserId().getValue());
        }
    }

    private Set<Long> loadLikedCommentIds(List<Comment> rootComments, List<Comment> replies, Long currentUserId) {
        if (currentUserId == null) {
            return Collections.<Long>emptySet();
        }
        Set<Long> commentIds = new HashSet<Long>();
        for (Comment comment : rootComments) {
            commentIds.add(comment.getId().getValue());
        }
        for (Comment reply : replies) {
            commentIds.add(reply.getId().getValue());
        }
        if (commentIds.isEmpty()) {
            return Collections.<Long>emptySet();
        }
        List<CommentLike> commentLikes = commentLikeRepository.findByCommentIdsAndUser(
            new ArrayList<Long>(commentIds),
            new UserId(currentUserId)
        );
        Set<Long> likedCommentIds = new HashSet<Long>();
        for (CommentLike commentLike : commentLikes) {
            likedCommentIds.add(commentLike.getCommentId().getValue());
        }
        return likedCommentIds;
    }

    private CommentDTO buildCommentDTO(Comment comment,
                                       Article article,
                                       Map<Long, User> userMap,
                                       Map<Long, Comment> parentCommentMap,
                                       Set<Long> likedCommentIds,
                                       Integer replyCount,
                                       List<Comment> previewReplies,
                                       Long currentUserId,
                                       String currentUserRole) {
        User user = userMap.get(comment.getUserId().getValue());
        CommentDTO dto = CommentAssembler.toDTO(comment, user);
        dto.setReplyCount(replyCount == null ? 0 : replyCount);
        dto.setLiked(likedCommentIds.contains(comment.getId().getValue()));
        dto.setCanDelete(canDelete(comment, article, currentUserId, currentUserRole));
        dto.setCanPin(canPin(comment, article, currentUserId, currentUserRole));
        dto.setCanEdit(canEdit(comment, currentUserId));
        dto.setAuthor(article.getAuthorId().getValue().equals(comment.getUserId().getValue()));
        dto.setReplyPreview(buildReplyPreview(previewReplies, article, userMap, parentCommentMap, likedCommentIds, currentUserId, currentUserRole));

        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Comment parentComment = parentCommentMap.get(comment.getParentId());
            if (parentComment != null) {
                User replyToUser = userMap.get(parentComment.getUserId().getValue());
                if (replyToUser != null) {
                    dto.setReplyToUser(toUserDTO(replyToUser));
                }
            }
        }
        return dto;
    }

    private List<CommentDTO> buildReplyPreview(List<Comment> previewReplies,
                                               Article article,
                                               Map<Long, User> userMap,
                                               Map<Long, Comment> parentCommentMap,
                                               Set<Long> likedCommentIds,
                                               Long currentUserId,
                                               String currentUserRole) {
        if (previewReplies == null || previewReplies.isEmpty()) {
            return new ArrayList<CommentDTO>();
        }
        List<CommentDTO> replyPreview = new ArrayList<CommentDTO>(previewReplies.size());
        for (Comment reply : previewReplies) {
            CommentDTO replyDTO = CommentAssembler.toDTO(reply, userMap.get(reply.getUserId().getValue()));
            replyDTO.setReplyCount(0);
            replyDTO.setLiked(likedCommentIds.contains(reply.getId().getValue()));
            replyDTO.setCanDelete(canDelete(reply, article, currentUserId, currentUserRole));
            replyDTO.setCanPin(Boolean.FALSE);
            replyDTO.setCanEdit(canEdit(reply, currentUserId));
            replyDTO.setAuthor(article.getAuthorId().getValue().equals(reply.getUserId().getValue()));
            replyDTO.setReplyPreview(new ArrayList<CommentDTO>());
            if (reply.getParentId() != null && reply.getParentId() > 0) {
                Comment parentComment = parentCommentMap.get(reply.getParentId());
                if (parentComment != null) {
                    User replyToUser = userMap.get(parentComment.getUserId().getValue());
                    if (replyToUser != null) {
                        replyDTO.setReplyToUser(toUserDTO(replyToUser));
                    }
                }
            }
            replyPreview.add(replyDTO);
        }
        return replyPreview;
    }

    private boolean canDelete(Comment comment, Article article, Long currentUserId, String currentUserRole) {
        if (currentUserId == null) {
            return false;
        }
        if (comment.getUserId().getValue().equals(currentUserId)) {
            return true;
        }
        if (article.getAuthorId().getValue().equals(currentUserId)) {
            return true;
        }
        return UserRole.ADMIN.name().equals(currentUserRole);
    }

    private boolean canPin(Comment comment, Article article, Long currentUserId, String currentUserRole) {
        if (currentUserId == null || !comment.isRootComment()) {
            return false;
        }
        return article.getAuthorId().getValue().equals(currentUserId);
    }

    private boolean canEdit(Comment comment, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        if (!comment.getUserId().getValue().equals(currentUserId)) {
            return false;
        }
        if (comment.getCreatedAt() == null) {
            return false;
        }
        return java.time.LocalDateTime.now().isBefore(comment.getCreatedAt().plusMinutes(10));
    }

    private UserDTO toUserDTO(User user) {
        return user == null ? null : com.myblog.application.assembler.UserAssembler.toDTO(user);
    }
}
