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
import com.myblog.shared.util.BizLogHelper;
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
        log.debug("{} | {} 处理浏览事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(0L),
            BizLogHelper.params("articleId", event.getArticleId()));
        try {
            articleRepository.incrementViewCount(event.getArticleId());
        } catch (Exception e) {
            log.error("{} | {} 处理浏览事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(0L),
                BizLogHelper.params("articleId", event.getArticleId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleLiked(ArticleLikedEvent event) {
        log.debug("{} | {} 处理点赞事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(event.getUserId()),
            BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()));
        try {
            articleRepository.incrementLikeCount(event.getArticleId());
        } catch (Exception e) {
            log.error("{} | {} 处理点赞事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleUnliked(ArticleUnlikedEvent event) {
        log.debug("{} | {} 处理取消点赞事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(event.getUserId()),
            BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()));
        try {
            articleRepository.decrementLikeCount(event.getArticleId());
        } catch (Exception e) {
            log.error("{} | {} 处理取消点赞事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleFavorited(ArticleFavoritedEvent event) {
        log.debug("{} | {} 处理收藏事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(event.getUserId()),
            BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()));
        try {
            articleRepository.incrementFavoriteCount(event.getArticleId());
        } catch (Exception e) {
            log.error("{} | {} 处理收藏事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onArticleUnfavorited(ArticleUnfavoritedEvent event) {
        log.debug("{} | {} 处理取消收藏事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(event.getUserId()),
            BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()));
        try {
            articleRepository.decrementFavoriteCount(event.getArticleId());
        } catch (Exception e) {
            log.error("{} | {} 处理取消收藏事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("articleId", event.getArticleId(), "userId", event.getUserId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentCreated(CommentCreatedEvent event) {
        log.debug("{} | {} 处理评论事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(0L),
            BizLogHelper.params("articleId", event.getArticleId(), "commentId", event.getCommentId()));
        try {
            articleRepository.incrementCommentCount(event.getArticleId());
        } catch (Exception e) {
            log.error("{} | {} 处理评论事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(0L),
                BizLogHelper.params("articleId", event.getArticleId(), "commentId", event.getCommentId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentDeleted(CommentDeletedEvent event) {
        log.debug("{} | {} 处理删除评论事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(0L),
            BizLogHelper.params("articleId", event.getArticleId(), "decrement", event.getDecrement()));
        try {
            articleRepository.decrementCommentCount(event.getArticleId(), event.getDecrement());
        } catch (Exception e) {
            log.error("{} | {} 处理删除评论事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(0L),
                BizLogHelper.params("articleId", event.getArticleId(), "decrement", event.getDecrement()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentLiked(CommentLikedEvent event) {
        log.debug("{} | {} 处理评论点赞事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(event.getUserId()),
            BizLogHelper.params("commentId", event.getCommentId(), "userId", event.getUserId()));
        try {
            commentRepository.incrementLikeCount(event.getCommentId());
        } catch (Exception e) {
            log.error("{} | {} 处理评论点赞事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("commentId", event.getCommentId(), "userId", event.getUserId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class)
    public void onCommentUnliked(CommentUnlikedEvent event) {
        log.debug("{} | {} 处理评论取消点赞事件 | 入参({})",
            BizLogHelper.trace(),
            BizLogHelper.who(event.getUserId()),
            BizLogHelper.params("commentId", event.getCommentId(), "userId", event.getUserId()));
        try {
            commentRepository.decrementLikeCount(event.getCommentId());
        } catch (Exception e) {
            log.error("{} | {} 处理评论取消点赞事件 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.who(event.getUserId()),
                BizLogHelper.params("commentId", event.getCommentId(), "userId", event.getUserId()),
                BizLogHelper.result("失败: " + e.getMessage()));
        }
    }
}
