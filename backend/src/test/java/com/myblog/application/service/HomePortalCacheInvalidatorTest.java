package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.service.HomeStatsAppService.HomeStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HomePortalCacheInvalidatorTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictFeaturedAndBootstrapClearsBothCachesImmediatelyWithoutTransaction() {
        Cache<String, HomeBootstrapDTO> bootstrapCache = Caffeine.newBuilder().build();
        Cache<String, List<ArticleDTO>> featuredCache = Caffeine.newBuilder().build();
        Cache<Long, HomeStats> statsCache = Caffeine.newBuilder().build();
        HomePortalCacheInvalidator invalidator = new HomePortalCacheInvalidator(
            bootstrapCache,
            featuredCache,
            statsCache
        );
        bootstrapCache.put(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY, new HomeBootstrapDTO());
        featuredCache.put("1:5", Collections.<ArticleDTO>emptyList());

        invalidator.evictFeaturedAndBootstrap();

        assertThat(bootstrapCache.getIfPresent(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY)).isNull();
        assertThat(featuredCache.getIfPresent("1:5")).isNull();
    }

    @Test
    void evictBootstrapRunsAfterCommitWhenTransactionSynchronizationIsActive() {
        Cache<String, HomeBootstrapDTO> bootstrapCache = Caffeine.newBuilder().build();
        Cache<String, List<ArticleDTO>> featuredCache = Caffeine.newBuilder().build();
        Cache<Long, HomeStats> statsCache = Caffeine.newBuilder().build();
        HomePortalCacheInvalidator invalidator = new HomePortalCacheInvalidator(
            bootstrapCache,
            featuredCache,
            statsCache
        );
        bootstrapCache.put(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY, new HomeBootstrapDTO());
        TransactionSynchronizationManager.initSynchronization();

        invalidator.evictBootstrap();

        assertThat(bootstrapCache.getIfPresent(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY)).isNotNull();
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        assertThat(bootstrapCache.getIfPresent(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY)).isNull();
    }

    @Test
    void evictStatsAndBootstrapClearsStatsAndBootstrap() {
        Cache<String, HomeBootstrapDTO> bootstrapCache = Caffeine.newBuilder().build();
        Cache<String, List<ArticleDTO>> featuredCache = Caffeine.newBuilder().build();
        Cache<Long, HomeStats> statsCache = Caffeine.newBuilder().build();
        HomePortalCacheInvalidator invalidator = new HomePortalCacheInvalidator(
            bootstrapCache,
            featuredCache,
            statsCache
        );
        bootstrapCache.put(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY, new HomeBootstrapDTO());
        statsCache.put(HomeStatsAppService.HOME_STATS_KEY, new HomeStats(1, 2, 3));

        invalidator.evictStatsAndBootstrap();

        assertThat(bootstrapCache.getIfPresent(HomeBootstrapAppService.BOOTSTRAP_CACHE_KEY)).isNull();
        assertThat(statsCache.getIfPresent(HomeStatsAppService.HOME_STATS_KEY)).isNull();
    }
}
