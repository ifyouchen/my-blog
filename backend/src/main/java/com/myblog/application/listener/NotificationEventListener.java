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
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.NotificationType;
import com.myblog.shared.enums.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
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
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationAppService notificationAppService;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationAppService notificationAppService,
                                     ArticleRepository articleRepository,
                                     CommentRepository commentRepository,
                                     UserRepository userRepository,
                                     ObjectMapper objectMapper) {
        this.notificationAppService = notificationAppService;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleLiked(ArticleLikedEvent event) {
        log.info("Processing ArticleLikedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            Optional<Article> articleOpt = articleRepository.findById(new ArticleId(event.getArticleId()));
            if (!articleOpt.isPresent()) {
                log.warn("Article {} not found for ArticleLikedEvent", event.getArticleId());
                return;
            }

            Article article = articleOpt.get();
            // If liking own article, don't create notification
            if (article.getAuthorId().getValue().equals(event.getUserId())) {
                log.debug("User {} liked own article {}, skipping notification", event.getUserId(), event.getArticleId());
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(article.getAuthorId().getValue());
            command.setActorUserId(event.getUserId());
            command.setType(NotificationType.ARTICLE_LIKE);
            command.setArticleId(event.getArticleId());
            command.setPayloadJson(buildArticlePayload(event.getArticleId(), event.getUserId()));

            notificationAppService.createNotification(command);
            log.info("Created ARTICLE_LIKE notification for article {} from user {}",
                event.getArticleId(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create notification for ArticleLikedEvent: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleFavorited(ArticleFavoritedEvent event) {
        log.info("Processing ArticleFavoritedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            Optional<Article> articleOpt = articleRepository.findById(new ArticleId(event.getArticleId()));
            if (!articleOpt.isPresent()) {
                log.warn("Article {} not found for ArticleFavoritedEvent", event.getArticleId());
                return;
            }

            Article article = articleOpt.get();
            // If favoriting own article, don't create notification
            if (article.getAuthorId().getValue().equals(event.getUserId())) {
                log.debug("User {} favorited own article {}, skipping notification",
                    event.getUserId(), event.getArticleId());
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(article.getAuthorId().getValue());
            command.setActorUserId(event.getUserId());
            command.setType(NotificationType.ARTICLE_FAVORITE);
            command.setArticleId(event.getArticleId());
            command.setPayloadJson(buildArticlePayload(event.getArticleId(), event.getUserId()));

            notificationAppService.createNotification(command);
            log.info("Created ARTICLE_FAVORITE notification for article {} from user {}",
                event.getArticleId(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create notification for ArticleFavoritedEvent: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        log.info("Processing CommentCreatedEvent for article {}, comment {}, author {}",
            event.getArticleId(), event.getCommentId(), event.getAuthorId());
        try {
            Optional<Comment> commentOpt = commentRepository.findById(new CommentId(event.getCommentId()));
            if (!commentOpt.isPresent()) {
                log.warn("Comment {} not found for CommentCreatedEvent", event.getCommentId());
                return;
            }

            Comment comment = commentOpt.get();
            Long receiverUserId;
            Long articleId = event.getArticleId();

            if (comment.isRootComment()) {
                // Root comment - notify article author
                Optional<Article> articleOpt = articleRepository.findById(new ArticleId(event.getArticleId()));
                if (!articleOpt.isPresent()) {
                    log.warn("Article {} not found for CommentCreatedEvent", event.getArticleId());
                    return;
                }
                receiverUserId = articleOpt.get().getAuthorId().getValue();
            } else {
                // Reply - notify the parent comment author
                if (comment.getParentId() == null) {
                    log.warn("Comment {} has no parent but is not root comment", event.getCommentId());
                    return;
                }
                Optional<Comment> parentOpt = commentRepository.findById(new CommentId(comment.getParentId()));
                if (!parentOpt.isPresent()) {
                    log.warn("Parent comment {} not found for CommentCreatedEvent", comment.getParentId());
                    return;
                }
                receiverUserId = parentOpt.get().getUserId().getValue();
            }

            // Don't notify if replying to own comment
            if (receiverUserId.equals(event.getAuthorId())) {
                log.debug("User {} replied to own comment, skipping notification", event.getAuthorId());
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
            log.info("Created {} notification for comment {} from user {} to user {}",
                command.getType(), event.getCommentId(), event.getAuthorId(), receiverUserId);
        } catch (Exception e) {
            log.error("Failed to create notification for CommentCreatedEvent: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentLiked(CommentLikedEvent event) {
        log.info("Processing CommentLikedEvent for comment {}, user {}", event.getCommentId(), event.getUserId());
        try {
            Optional<Comment> commentOpt = commentRepository.findById(new CommentId(event.getCommentId()));
            if (!commentOpt.isPresent()) {
                log.warn("Comment {} not found for CommentLikedEvent", event.getCommentId());
                return;
            }

            Comment comment = commentOpt.get();
            // If liking own comment, don't create notification
            if (comment.getUserId().getValue().equals(event.getUserId())) {
                log.debug("User {} liked own comment {}, skipping notification",
                    event.getUserId(), event.getCommentId());
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
            log.info("Created COMMENT_LIKE notification for comment {} from user {}",
                event.getCommentId(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create notification for CommentLikedEvent: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserFollowed(UserFollowedEvent event) {
        log.info("Processing UserFollowedEvent: user {} followed user {}",
            event.getFollowerUserId(), event.getFollowingUserId());
        try {
            // Don't notify if following yourself (though business logic should prevent this)
            if (event.getFollowerUserId().equals(event.getFollowingUserId())) {
                log.debug("User {} followed themselves, skipping notification", event.getFollowerUserId());
                return;
            }

            CreateNotificationCommand command = new CreateNotificationCommand();
            command.setReceiverUserId(event.getFollowingUserId());
            command.setActorUserId(event.getFollowerUserId());
            command.setType(NotificationType.USER_FOLLOW);
            command.setPayloadJson(buildUserPayload(event.getFollowerUserId()));

            notificationAppService.createNotification(command);
            log.info("Created USER_FOLLOW notification from user {} to user {}",
                event.getFollowerUserId(), event.getFollowingUserId());
        } catch (Exception e) {
            log.error("Failed to create notification for UserFollowedEvent: {}", e.getMessage());
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
            Optional<Comment> commentOpt = commentRepository.findById(new CommentId(commentId));
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