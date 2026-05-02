package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.shared.util.BizLogHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class HomeStatsAppService {

    private static final Logger log = LoggerFactory.getLogger(HomeStatsAppService.class);
    private static final Long HOME_STATS_KEY = 1L;

    private final ArticleRepository articleRepository;
    private final ColumnRepository columnRepository;
    private final Cache<Long, HomeStats> homeStatsCache;

    public HomeStatsAppService(ArticleRepository articleRepository,
                               ColumnRepository columnRepository,
                               @Qualifier("homeStatsCache") Cache<Long, HomeStats> homeStatsCache) {
        this.articleRepository = articleRepository;
        this.columnRepository = columnRepository;
        this.homeStatsCache = homeStatsCache;
    }

    @PostConstruct
    public void init() {
        long _start = System.currentTimeMillis();
        refreshStats();
        log.info("{} | 系统 初始化首页缓存 | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.result("OK"),
            BizLogHelper.elapsed(_start));
    }

    public HomeStats getStats() {
        return homeStatsCache.get(HOME_STATS_KEY, k -> loadStats());
    }

    @Scheduled(fixedRate = 180000)
    public void refreshStats() {
        long _start = System.currentTimeMillis();
        HomeStats stats = loadStats();
        homeStatsCache.put(HOME_STATS_KEY, stats);
        log.info("{} | 系统 刷新首页缓存 | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.result("articles=" + stats.totalArticles + ", authors=" + stats.totalAuthors + ", columns=" + stats.totalColumns),
            BizLogHelper.elapsed(_start));
    }

    private HomeStats loadStats() {
        long totalArticles = articleRepository.countPublished();
        long totalAuthors = articleRepository.countPublishedAuthors();
        long totalColumns = columnRepository.countPublished();
        return new HomeStats(totalArticles, totalAuthors, totalColumns);
    }

    public static class HomeStats {
        private final long totalArticles;
        private final long totalAuthors;
        private final long totalColumns;

        public HomeStats(long totalArticles, long totalAuthors, long totalColumns) {
            this.totalArticles = totalArticles;
            this.totalAuthors = totalAuthors;
            this.totalColumns = totalColumns;
        }

        public long getTotalArticles() {
            return totalArticles;
        }

        public long getTotalAuthors() {
            return totalAuthors;
        }

        public long getTotalColumns() {
            return totalColumns;
        }
    }
}
