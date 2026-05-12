package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.query.RecommendArticleCacheKey;
import com.myblog.application.service.HomeStatsAppService.HomeStats;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.shared.result.PageResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 首页 Portal 相关缓存统一失效入口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class HomePortalCacheInvalidator {

    private final Cache<String, HomeBootstrapDTO> homeBootstrapCache;
    private final Cache<String, List<ArticleDTO>> featuredArticlesCache;
    private final Cache<Long, HomeStats> homeStatsCache;
    private final Cache<RecommendArticleCacheKey, PageResult<Article>> recommendedArticleFeedCache;

    public HomePortalCacheInvalidator(
            @Qualifier("homeBootstrapCache") Cache<String, HomeBootstrapDTO> homeBootstrapCache,
            @Qualifier("featuredArticlesCache") Cache<String, List<ArticleDTO>> featuredArticlesCache,
            @Qualifier("homeStatsCache") Cache<Long, HomeStats> homeStatsCache,
            @Qualifier("recommendedArticleFeedCache")
            Cache<RecommendArticleCacheKey, PageResult<Article>> recommendedArticleFeedCache) {
        this.homeBootstrapCache = homeBootstrapCache;
        this.featuredArticlesCache = featuredArticlesCache;
        this.homeStatsCache = homeStatsCache;
        this.recommendedArticleFeedCache = recommendedArticleFeedCache;
    }

    /**
     * 失效首页启动缓存（事务提交后执行）。
     */
    public void evictBootstrap() {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                homeBootstrapCache.invalidate(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY);
            }
        });
    }

    /**
     * 失效精选文章缓存、推荐动态流缓存和首页启动缓存（事务提交后执行）。
     * <p>精选文章发生可见性变化时调用。</p>
     */
    public void evictFeaturedAndBootstrap() {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                featuredArticlesCache.invalidateAll();
                recommendedArticleFeedCache.invalidateAll();
                homeBootstrapCache.invalidate(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY);
            }
        });
    }

    /**
     * 失效首页统计缓存和首页启动缓存（事务提交后执行）。
     * <p>文章发布可见性变化时调用。</p>
     */
    public void evictStatsAndBootstrap() {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                homeStatsCache.invalidate(HomeStatsAppService.HOME_STATS_KEY);
                homeBootstrapCache.invalidate(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY);
            }
        });
    }

    /**
     * 失效推荐文章动态流缓存和首页启动缓存（事务提交后执行）。
     */
    public void evictRecommendedArticles() {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                recommendedArticleFeedCache.invalidateAll();
                homeBootstrapCache.invalidate(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY);
            }
        });
    }

    /**
     * 如果当前处于事务中，则在事务提交后执行指定操作；否则立即执行。
     *
     * @param action 待执行的操作
     */
    private void afterCommit(final Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
