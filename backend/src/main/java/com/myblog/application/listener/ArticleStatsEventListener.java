package com.myblog.application.listener;

import com.myblog.application.event.ArticleFavoritedEvent;
import com.myblog.application.event.ArticleLikedEvent;
import com.myblog.application.event.ArticleUnfavoritedEvent;
import com.myblog.application.event.ArticleUnlikedEvent;
import com.myblog.application.event.ArticleViewedEvent;
import com.myblog.application.event.CommentCreatedEvent;
import com.myblog.application.event.CommentDeletedEvent;
import com.myblog.application.event.CommentLikedEvent;
import com.myblog.application.event.CommentUnlikedEvent;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文章 / 评论统计数据事件监听器。
 *
 * <p><b>并发安全设计说明：</b><br>
 * 所有计数更新均通过数据库原子 SQL（{@code like_count = like_count + 1}）完成，
 * 彻底避免了「Read-Modify-Write」模式在并发场景下的丢失更新问题。
 * 不再使用领域模型中的 {@code increaseLikeCount()} / {@code decreaseLikeCount()} 等方法
 * （那些方法仍保留供单线程/测试场景使用）。
 * </p>
 */
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
    @Transactional(rollbackFor = Exception.class)
    public void onArticleViewed(ArticleViewedEvent event) {
        log.debug("Processing ArticleViewedEvent for article {}", event.getArticleId());
        try {
            articleRepository.incrementViewCount(event.getArticleId());
        } catch (Exception e) {
            log.error("Failed to increment view count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleLiked(ArticleLikedEvent event) {
        log.debug("Processing ArticleLikedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.incrementLikeCount(event.getArticleId());
        } catch (Exception e) {
            log.error("Failed to increment like count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleUnliked(ArticleUnlikedEvent event) {
        log.debug("Processing ArticleUnlikedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.decrementLikeCount(event.getArticleId());
        } catch (Exception e) {
            log.error("Failed to decrement like count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleFavorited(ArticleFavoritedEvent event) {
        log.debug("Processing ArticleFavoritedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.incrementFavoriteCount(event.getArticleId());
        } catch (Exception e) {
            log.error("Failed to increment favorite count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleUnfavorited(ArticleUnfavoritedEvent event) {
        log.debug("Processing ArticleUnfavoritedEvent for article {}, user {}", event.getArticleId(), event.getUserId());
        try {
            articleRepository.decrementFavoriteCount(event.getArticleId());
        } catch (Exception e) {
            log.error("Failed to decrement favorite count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentCreated(CommentCreatedEvent event) {
        log.debug("Processing CommentCreatedEvent for article {}, comment {}", event.getArticleId(), event.getCommentId());
        try {
            articleRepository.incrementCommentCount(event.getArticleId());
        } catch (Exception e) {
            log.error("Failed to increment comment count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentDeleted(CommentDeletedEvent event) {
        log.debug("Processing CommentDeletedEvent for article {}, decrement={}", event.getArticleId(), event.getDecrement());
        try {
            articleRepository.decrementCommentCount(event.getArticleId(), event.getDecrement());
        } catch (Exception e) {
            log.error("Failed to decrement comment count for article {}: {}", event.getArticleId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentLiked(CommentLikedEvent event) {
        log.debug("Processing CommentLikedEvent for comment {}, user {}", event.getCommentId(), event.getUserId());
        try {
            commentRepository.incrementLikeCount(event.getCommentId());
        } catch (Exception e) {
            log.error("Failed to increment like count for comment {}: {}", event.getCommentId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentUnliked(CommentUnlikedEvent event) {
        log.debug("Processing CommentUnlikedEvent for comment {}, user {}", event.getCommentId(), event.getUserId());
        try {
            commentRepository.decrementLikeCount(event.getCommentId());
        } catch (Exception e) {
            log.error("Failed to decrement like count for comment {}: {}", event.getCommentId(), e.getMessage());
        }
    }
}
