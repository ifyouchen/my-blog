package com.myblog.application.listener;

import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.application.service.HomePortalCacheInvalidator;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class StatsAsyncHandler {

    private static final Logger log = LoggerFactory.getLogger(StatsAsyncHandler.class);

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final HomePortalCacheInvalidator homePortalCacheInvalidator;

    public StatsAsyncHandler(ArticleRepository articleRepository,
                             CommentRepository commentRepository,
                             HomePortalCacheInvalidator homePortalCacheInvalidator) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.homePortalCacheInvalidator = homePortalCacheInvalidator;
    }

    @Async("statsAsyncExecutor")
    public void incrementViewCount(Long articleId) {
        try {
            articleRepository.incrementViewCount(articleId);
            articleRepository.recordArticleView(articleId);
            homePortalCacheInvalidator.evictRecommendedArticles();
        } catch (Exception e) {
            log.error("{} | 异步递增浏览数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("articleId", articleId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void incrementLikeCount(Long articleId) {
        try {
            articleRepository.incrementLikeCount(articleId);
            homePortalCacheInvalidator.evictRecommendedArticles();
        } catch (Exception e) {
            log.error("{} | 异步递增点赞数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("articleId", articleId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void decrementLikeCount(Long articleId) {
        try {
            articleRepository.decrementLikeCount(articleId);
            homePortalCacheInvalidator.evictRecommendedArticles();
        } catch (Exception e) {
            log.error("{} | 异步递减点赞数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("articleId", articleId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void incrementFavoriteCount(Long articleId) {
        try {
            articleRepository.incrementFavoriteCount(articleId);
            homePortalCacheInvalidator.evictRecommendedArticles();
        } catch (Exception e) {
            log.error("{} | 异步递增收藏数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("articleId", articleId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void decrementFavoriteCount(Long articleId) {
        try {
            articleRepository.decrementFavoriteCount(articleId);
            homePortalCacheInvalidator.evictRecommendedArticles();
        } catch (Exception e) {
            log.error("{} | 异步递减收藏数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("articleId", articleId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void incrementCommentCount(Long articleId) {
        try {
            articleRepository.incrementCommentCount(articleId);
            homePortalCacheInvalidator.evictRecommendedArticles();
        } catch (Exception e) {
            log.error("{} | 异步递增评论数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("articleId", articleId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void decrementCommentCount(Long articleId, int decrement) {
        try {
            articleRepository.decrementCommentCount(articleId, decrement);
            homePortalCacheInvalidator.evictRecommendedArticles();
        } catch (Exception e) {
            log.error("{} | 异步递减评论数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("articleId", articleId, "decrement", decrement),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void incrementCommentLikeCount(Long commentId) {
        try {
            commentRepository.incrementLikeCount(commentId);
        } catch (Exception e) {
            log.error("{} | 异步递增评论点赞数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("commentId", commentId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }

    @Async("statsAsyncExecutor")
    public void decrementCommentLikeCount(Long commentId) {
        try {
            commentRepository.decrementLikeCount(commentId);
        } catch (Exception e) {
            log.error("{} | 异步递减评论点赞数失败 | 入参({}) | 结果({})",
                BizLogHelper.trace(),
                BizLogHelper.params("commentId", commentId),
                BizLogHelper.result("失败: " + e.getMessage()),
                e);
        }
    }
}
