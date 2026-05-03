package com.myblog.application.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.application.command.CreateNotificationCommand;
import com.myblog.application.event.*;
import com.myblog.application.service.NotificationAppService;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.NotificationType;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知事件监听器。
 * 监听各类事件并创建相应的通知。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
@Async
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationAppService notificationAppService;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationAppService notificationAppService,
                                     ArticleRepository articleRepository,
                                     CommentRepository commentRepository,
                                     UserRepository userRepository,
                                     UserFollowRepository userFollowRepository,
                                     ObjectMapper objectMapper) {
        this.notificationAppService = notificationAppService;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleLiked(ArticleLikedEvent event) {
        long _start = System.currentTimeMillis();
        log.info("{} | {} 处理点赞事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(event.getUserId()),
            BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()));
        try {
            Optional<Article> articleOpt = articleRepository.findById(new ArticleId(event.getArticleId()));
            if (!articleOpt.isPresent()) {
                log.warn("{} | {} 处理点赞事件 | 入参({}) | 结果({}) | {}",
                    BizLogHelper.trace(),
                    BizLogHelper.who(event.getUserId()),
                    BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                    BizLogHelper.result("文章不存在"),
                    BizLogHelper.elapsed(_start));
                return;
            }

            Article article = articleOpt.get();
            // If liking own article, don't create notification
            if (article.getAuthorId().getValue().equals(event.getUserId())) {
                log.debug("{} | {} 处理点赞事件 | 入参({}) | 结果({}) | {}",
                    BizLogHelper.trace(),
                    BizLogHelper.who(event.getUserId()),
                    BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                    BizLogHelper.result("自己的文章，跳过通知"),
                    BizLogHelper.elapsed(_start));
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(article.getAuthorId().getValue());
            command.setActorUserId(event.getUserId());
            command.setType(NotificationType.ARTICLE_LIKE);
            command.setArticleId(event.getArticleId());
            command.setPayloadJson(buildArticlePayload(event.getArticleId(), event.getUserId()));

            notificationAppService.createNotification(command);
            log.info("{} | {} 处理点赞事件 | 入参({}) | 结果(type=ARTICLE_LIKE, articleId={}, fromUser={}, toUser={}) | {}",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                event.getArticleId(), event.getUserId(), article.getAuthorId().getValue(),
                BizLogHelper.elapsed(_start));
        } catch (Exception e) {
            log.error("{} | {} 处理点赞事件 | 入参({}) | 结果({}) | {}",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                BizLogHelper.result("失败: " + e.getMessage()),
                BizLogHelper.elapsed(_start));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleFavorited(ArticleFavoritedEvent event) {
        long _start = System.currentTimeMillis();
        String who = BizLogHelper.who(event.getUserId());
        String params = BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId());
        log.info("{} | {} 处理收藏事件 | 入参({})", BizLogHelper.trace(), who, params);
        try {
            Optional<Article> articleOpt = articleRepository.findById(new ArticleId(event.getArticleId()));
            if (!articleOpt.isPresent()) {
                log.warn("{} | {} 处理收藏事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("文章不存在"), BizLogHelper.elapsed(_start));
                return;
            }

            Article article = articleOpt.get();
            // If favoriting own article, don't create notification
            if (article.getAuthorId().getValue().equals(event.getUserId())) {
                log.debug("{} | {} 处理收藏事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("自己的文章，跳过通知"), BizLogHelper.elapsed(_start));
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(article.getAuthorId().getValue());
            command.setActorUserId(event.getUserId());
            command.setType(NotificationType.ARTICLE_FAVORITE);
            command.setArticleId(event.getArticleId());
            command.setPayloadJson(buildArticlePayload(event.getArticleId(), event.getUserId()));

            notificationAppService.createNotification(command);
            log.info("{} | {} 处理收藏事件 | 入参({}) | 结果(type=ARTICLE_FAVORITE, articleId={}, fromUser={}) | {}", BizLogHelper.trace(), who, params,
                event.getArticleId(), event.getUserId(), BizLogHelper.elapsed(_start));
        } catch (Exception e) {
            log.error("{} | {} 处理收藏事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                BizLogHelper.result("失败: " + e.getMessage()), BizLogHelper.elapsed(_start));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        long _start = System.currentTimeMillis();
        String who = BizLogHelper.who(event.getAuthorId());
        String params = BizLogHelper.params("articleId", event.getArticleId(), "commentId", event.getCommentId(), "authorId", event.getAuthorId());
        log.info("{} | {} 处理评论事件 | 入参({})", BizLogHelper.trace(), who, params);
        try {
            Optional<Comment> commentOpt = commentRepository.findByIdForAdmin(new CommentId(event.getCommentId()));
            if (!commentOpt.isPresent()) {
                log.warn("{} | {} 处理评论事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("评论不存在"), BizLogHelper.elapsed(_start));
                return;
            }

            Comment comment = commentOpt.get();
            if (!comment.isPublished()) {
                log.debug("{} | {} 处理评论事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("评论未发布，跳过通知"), BizLogHelper.elapsed(_start));
                return;
            }

            Long receiverUserId;
            Long articleId = event.getArticleId();

            if (comment.isRootComment()) {
                // Root comment - notify article author
                Optional<Article> articleOpt = articleRepository.findById(new ArticleId(event.getArticleId()));
                if (!articleOpt.isPresent()) {
                    log.warn("{} | {} 处理评论事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                        BizLogHelper.result("文章不存在"), BizLogHelper.elapsed(_start));
                    return;
                }
                receiverUserId = articleOpt.get().getAuthorId().getValue();
            } else {
                // Reply - notify the parent comment author
                if (comment.getParentId() == null) {
                    log.warn("{} | {} 处理评论事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                        BizLogHelper.result("父评论不存在"), BizLogHelper.elapsed(_start));
                    return;
                }
                Optional<Comment> parentOpt = commentRepository.findById(new CommentId(comment.getParentId()));
                if (!parentOpt.isPresent()) {
                    log.warn("{} | {} 处理评论事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                        BizLogHelper.result("父评论不存在"), BizLogHelper.elapsed(_start));
                    return;
                }
                receiverUserId = parentOpt.get().getUserId().getValue();
            }

            // Don't notify if replying to own comment
            if (receiverUserId.equals(event.getAuthorId())) {
                log.debug("{} | {} 处理评论事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("自己的评论，跳过通知"), BizLogHelper.elapsed(_start));
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(receiverUserId);
            command.setActorUserId(event.getAuthorId());
            command.setType(comment.isRootComment() ? NotificationType.ARTICLE_COMMENT : NotificationType.COMMENT_REPLY);
            command.setArticleId(articleId);
            command.setCommentId(event.getCommentId());
            command.setPayloadJson(buildCommentPayload(event.getArticleId(), event.getCommentId(), event.getAuthorId()));

            notificationAppService.createNotification(command);
            log.info("{} | {} 处理评论事件 | 入参({}) | 结果(type={}, commentId={}, fromUser={}, toUser={}) | {}", BizLogHelper.trace(), who, params,
                command.getType(), event.getCommentId(), event.getAuthorId(), receiverUserId, BizLogHelper.elapsed(_start));
        } catch (Exception e) {
            log.error("{} | {} 处理评论事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                BizLogHelper.result("失败: " + e.getMessage()), BizLogHelper.elapsed(_start));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentLiked(CommentLikedEvent event) {
        long _start = System.currentTimeMillis();
        String who = BizLogHelper.who(event.getUserId());
        String params = BizLogHelper.params("commentId", event.getCommentId(), "userId", event.getUserId());
        log.info("{} | {} 处理评论点赞事件 | 入参({})", BizLogHelper.trace(), who, params);
        try {
            Optional<Comment> commentOpt = commentRepository.findById(new CommentId(event.getCommentId()));
            if (!commentOpt.isPresent()) {
                log.warn("{} | {} 处理评论点赞事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("评论不存在"), BizLogHelper.elapsed(_start));
                return;
            }

            Comment comment = commentOpt.get();
            // If liking own comment, don't create notification
            if (comment.getUserId().getValue().equals(event.getUserId())) {
                log.debug("{} | {} 处理评论点赞事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("自己的评论，跳过通知"), BizLogHelper.elapsed(_start));
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(comment.getUserId().getValue());
            command.setActorUserId(event.getUserId());
            command.setType(NotificationType.COMMENT_LIKE);
            command.setArticleId(comment.getArticleId().getValue());
            command.setCommentId(event.getCommentId());
            command.setPayloadJson(buildCommentPayload(comment.getArticleId().getValue(), event.getCommentId(), event.getUserId()));

            notificationAppService.createNotification(command);
            log.info("{} | {} 处理评论点赞事件 | 入参({}) | 结果(type=COMMENT_LIKE, commentId={}, fromUser={}) | {}", BizLogHelper.trace(), who, params,
                event.getCommentId(), event.getUserId(), BizLogHelper.elapsed(_start));
        } catch (Exception e) {
            log.error("{} | {} 处理评论点赞事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                BizLogHelper.result("失败: " + e.getMessage()), BizLogHelper.elapsed(_start));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserFollowed(UserFollowedEvent event) {
        long _start = System.currentTimeMillis();
        String who = BizLogHelper.who(event.getFollowerUserId());
        String params = BizLogHelper.params("followerUserId", event.getFollowerUserId(), "followingUserId", event.getFollowingUserId());
        log.info("{} | {} 处理关注事件 | 入参({})", BizLogHelper.trace(), who, params);
        try {
            // Don't notify if following yourself (though business logic should prevent this)
            if (event.getFollowerUserId().equals(event.getFollowingUserId())) {
                log.debug("{} | {} 处理关注事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("关注自己，跳过通知"), BizLogHelper.elapsed(_start));
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(event.getFollowingUserId());
            command.setActorUserId(event.getFollowerUserId());
            command.setType(NotificationType.USER_FOLLOW);
            command.setPayloadJson(buildUserPayload(event.getFollowerUserId()));

            notificationAppService.createNotification(command);
            log.info("{} | {} 处理关注事件 | 入参({}) | 结果(type=USER_FOLLOW, fromUser={}, toUser={}) | {}", BizLogHelper.trace(), who, params,
                event.getFollowerUserId(), event.getFollowingUserId(), BizLogHelper.elapsed(_start));
        } catch (Exception e) {
            log.error("{} | {} 处理关注事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                BizLogHelper.result("失败: " + e.getMessage()), BizLogHelper.elapsed(_start));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticlePublished(ArticlePublishedEvent event) {
        long _start = System.currentTimeMillis();
        String who = BizLogHelper.who(event.getAuthorId());
        String params = BizLogHelper.params("articleId", event.getArticleId(), "authorId", event.getAuthorId());
        log.info("{} | {} 处理发布事件 | 入参({})", BizLogHelper.trace(), who, params);
        try {
            Optional<Article> articleOpt = articleRepository.findById(new ArticleId(event.getArticleId()));
            if (!articleOpt.isPresent()) {
                log.warn("{} | {} 处理发布事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("文章不存在"), BizLogHelper.elapsed(_start));
                return;
            }

            Long authorId = event.getAuthorId();
            List<Long> followerIds = userFollowRepository.findFollowerUserIds(new UserId(authorId));

            if (followerIds.isEmpty()) {
                log.debug("{} | {} 处理发布事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                    BizLogHelper.result("没有粉丝，跳过通知"), BizLogHelper.elapsed(_start));
                return;
            }

            int successCount = 0;
            for (Long followerId : followerIds) {
                if (followerId.equals(authorId)) {
                    continue;
                }
                try {
                    CreateNotificationCommand command = new CreateNotificationCommand();
                    command.setReceiverUserId(followerId);
                    command.setActorUserId(authorId);
                    command.setType(NotificationType.ARTICLE_PUBLISH);
                    command.setArticleId(event.getArticleId());
                    command.setPayloadJson(buildArticlePayload(event.getArticleId(), authorId));

                    notificationAppService.createNotification(command);
                    successCount++;
                } catch (Exception e) {
                    log.error("{} | {} 处理发布事件 | 入参({}) | 结果(通知粉丝{}失败: {}) | {}", BizLogHelper.trace(), who, params,
                        followerId, e.getMessage(), BizLogHelper.elapsed(_start));
                }
            }

            log.info("{} | {} 处理发布事件 | 入参({}) | 结果(成功通知{}个粉丝) | {}", BizLogHelper.trace(), who, params,
                successCount, BizLogHelper.elapsed(_start));
        } catch (Exception e) {
            log.error("{} | {} 处理发布事件 | 入参({}) | 结果({}) | {}", BizLogHelper.trace(), who, params,
                BizLogHelper.result("失败: " + e.getMessage()), BizLogHelper.elapsed(_start));
        }
    }

    private String buildArticlePayload(Long articleId, Long actorUserId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            Optional<Article> articleOpt = articleRepository.findById(new ArticleId(articleId));
            if (articleOpt.isPresent()) {
                Article article = articleOpt.get();
                payload.put("articleTitle", article.getTitle());
            }
            Optional<User> actorOpt = userRepository.findById(new UserId(actorUserId));
            if (actorOpt.isPresent()) {
                User actor = actorOpt.get();
                payload.put("actorName", actor.getNickname());
                payload.put("actorAvatar", actor.getAvatarUrl());
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to build article payload: {}", e.getMessage());
            return "{}";
        }
    }

    private String buildCommentPayload(Long articleId, Long commentId, Long actorUserId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            Optional<Article> articleOpt = articleRepository.findById(new ArticleId(articleId));
            if (articleOpt.isPresent()) {
                payload.put("articleTitle", articleOpt.get().getTitle());
            }
            Optional<Comment> commentOpt = commentRepository.findByIdForAdmin(new CommentId(commentId));
            if (commentOpt.isPresent()) {
                String content = commentOpt.get().getContent();
                payload.put("commentExcerpt", content.length() > 50 ? content.substring(0, 50) + "..." : content);
            }
            Optional<User> actorOpt = userRepository.findById(new UserId(actorUserId));
            if (actorOpt.isPresent()) {
                User actor = actorOpt.get();
                payload.put("actorName", actor.getNickname());
                payload.put("actorAvatar", actor.getAvatarUrl());
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to build comment payload: {}", e.getMessage());
            return "{}";
        }
    }

    private String buildUserPayload(Long actorUserId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            Optional<User> actorOpt = userRepository.findById(new UserId(actorUserId));
            if (actorOpt.isPresent()) {
                User actor = actorOpt.get();
                payload.put("actorName", actor.getNickname());
                payload.put("actorAvatar", actor.getAvatarUrl());
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to build user payload: {}", e.getMessage());
            return "{}";
        }
    }
}
