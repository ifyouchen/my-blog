package com.myblog.application.listener;

import com.myblog.application.event.*;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Async
public class ArticleStatsEventListener {

    private static final Logger log = LoggerFactory.getLogger(ArticleStatsEventListener.class);

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    public ArticleStatsEventListener(ArticleRepository articleRepository,
                                     CommentRepository commentRepository) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleViewed(ArticleViewedEvent event) {
        log.info("Processing ArticleViewedEvent for article {}", event.getArticleId());
        try {
            articleRepository.findById(new ArticleId(event.getArticleId()))
                .ifPresent(article -> {
                    article.increaseViewCount();
                    articleRepository.save(article);
                    log.debug("Article {} view count increased to {}", article.getId().getValue(), article.getViewCount());
                });
        } catch (Exception e) {
            log.error("Failed to increase view count for article {}: {}",
                event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleLiked(ArticleLikedEvent event) {
        log.info("Processing ArticleLikedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.findById(new ArticleId(event.getArticleId()))
                .ifPresent(article -> {
                    article.increaseLikeCount();
                    articleRepository.save(article);
                    log.debug("Article {} like count increased to {}", article.getId().getValue(), article.getLikeCount());
                });
        } catch (Exception e) {
            log.error("Failed to increase like count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleUnliked(ArticleUnlikedEvent event) {
        log.info("Processing ArticleUnlikedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.findById(new ArticleId(event.getArticleId()))
                .ifPresent(article -> {
                    article.decreaseLikeCount();
                    articleRepository.save(article);
                    log.debug("Article {} like count decreased to {}", article.getId().getValue(), article.getLikeCount());
                });
        } catch (Exception e) {
            log.error("Failed to decrease like count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleFavorited(ArticleFavoritedEvent event) {
        log.info("Processing ArticleFavoritedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.findById(new ArticleId(event.getArticleId()))
                .ifPresent(article -> {
                    article.increaseFavoriteCount();
                    articleRepository.save(article);
                    log.debug("Article {} favorite count increased to {}", article.getId().getValue(), article.getFavoriteCount());
                });
        } catch (Exception e) {
            log.error("Failed to increase favorite count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleUnfavorited(ArticleUnfavoritedEvent event) {
        log.info("Processing ArticleUnfavoritedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.findById(new ArticleId(event.getArticleId()))
                .ifPresent(article -> {
                    article.decreaseFavoriteCount();
                    articleRepository.save(article);
                    log.debug("Article {} favorite count decreased to {}", article.getId().getValue(), article.getFavoriteCount());
                });
        } catch (Exception e) {
            log.error("Failed to decrease favorite count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        log.info("Processing CommentCreatedEvent for article {}, comment {}", event.getArticleId(), event.getCommentId());
        try {
            articleRepository.findById(new ArticleId(event.getArticleId()))
                .ifPresent(article -> {
                    article.increaseCommentCount();
                    articleRepository.save(article);
                    log.debug("Article {} comment count increased to {}", article.getId().getValue(), article.getCommentCount());
                });
        } catch (Exception e) {
            log.error("Failed to increase comment count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentDeleted(CommentDeletedEvent event) {
        log.info("Processing CommentDeletedEvent for article {}, comment {}, decrement={}",
            event.getArticleId(), event.getCommentId(), event.getDecrement());
        try {
            articleRepository.findById(new ArticleId(event.getArticleId()))
                .ifPresent(article -> {
                    for (int i = 0; i < event.getDecrement(); i++) {
                        article.decreaseCommentCount();
                    }
                    articleRepository.save(article);
                    log.debug("Article {} comment count decreased by {} to {}",
                        article.getId().getValue(), event.getDecrement(), article.getCommentCount());
                });
        } catch (Exception e) {
            log.error("Failed to decrease comment count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentLiked(CommentLikedEvent event) {
        log.info("Processing CommentLikedEvent for comment {}, user {}", event.getCommentId(), event.getUserId());
        try {
            commentRepository.findById(new CommentId(event.getCommentId()))
                .ifPresent(comment -> {
                    comment.increaseLikeCount();
                    commentRepository.save(comment);
                    log.debug("Comment {} like count increased to {}", comment.getId().getValue(), comment.getLikeCount());
                });
        } catch (Exception e) {
            log.error("Failed to increase like count for comment {}: {}", event.getCommentId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentUnliked(CommentUnlikedEvent event) {
        log.info("Processing CommentUnlikedEvent for comment {}, user {}", event.getCommentId(), event.getUserId());
        try {
            commentRepository.findById(new CommentId(event.getCommentId()))
                .ifPresent(comment -> {
                    comment.decreaseLikeCount();
                    commentRepository.save(comment);
                    log.debug("Comment {} like count decreased to {}", comment.getId().getValue(), comment.getLikeCount());
                });
        } catch (Exception e) {
            log.error("Failed to decrease like count for comment {}: {}", event.getCommentId(), e.getMessage());
        }
    }
}
