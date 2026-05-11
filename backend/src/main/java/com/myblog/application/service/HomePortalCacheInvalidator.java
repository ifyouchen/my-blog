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

    public void evictBootstrap() {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                homeBootstrapCache.invalidate(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY);
            }
        });
    }

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

    public void evictStatsAndBootstrap() {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                homeStatsCache.invalidate(HomeStatsAppService.HOME_STATS_KEY);
                homeBootstrapCache.invalidate(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY);
            }
        });
    }

    public void evictRecommendedArticles() {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                recommendedArticleFeedCache.invalidateAll();
                homeBootstrapCache.invalidate(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY);
            }
        });
    }

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
