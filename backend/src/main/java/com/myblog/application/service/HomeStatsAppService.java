package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 首页统计数据应用服务。
 * <p>
 * 缓存首页展示的全局统计指标（已发布文章数、作者数、专栏数），
 * 应用启动时预热并每 3 分钟自动刷新。
 * </p>
 */
@Service
@DependsOn("cacheConfig")
public class HomeStatsAppService {

    private static final Logger log = LoggerFactory.getLogger(HomeStatsAppService.class);
    static final Long HOME_STATS_KEY = 1L;

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

    /**
     * 应用启动完成后预热首页统计缓存。
     */
    @PostConstruct
    public void init() {
        long _start = System.currentTimeMillis();
        refreshStats();
        log.info("{} | 系统 初始化首页缓存 | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.result("OK"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 获取首页统计数据，优先从缓存返回。
     *
     * @return 首页统计对象
     */
    public HomeStats getStats() {
        return homeStatsCache.get(HOME_STATS_KEY, k -> loadStats());
    }

    /**
     * 定时刷新首页统计缓存（每 3 分钟执行一次）。
     */
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

    /**
     * 从数据库加载首页统计数据。
     *
     * @return 首页统计对象
     */
    private HomeStats loadStats() {
        long totalArticles = articleRepository.countPublished();
        long totalAuthors = articleRepository.countPublishedAuthors();
        long totalColumns = columnRepository.countPublished();
        return new HomeStats(totalArticles, totalAuthors, totalColumns);
    }

    /**
     * 首页统计数据值对象。
     */
    public static class HomeStats {
        private final long totalArticles;
        private final long totalAuthors;
        private final long totalColumns;

        /**
         * 构建首页统计。
         *
         * @param totalArticles 已发布文章总数
         * @param totalAuthors  有文章的作者数
         * @param totalColumns  已发布专栏总数
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public HomeStats(@com.fasterxml.jackson.annotation.JsonProperty("totalArticles") long totalArticles,
                         @com.fasterxml.jackson.annotation.JsonProperty("totalAuthors") long totalAuthors,
                         @com.fasterxml.jackson.annotation.JsonProperty("totalColumns") long totalColumns) {
            this.totalArticles = totalArticles;
            this.totalAuthors = totalAuthors;
            this.totalColumns = totalColumns;
        }

        /**
         * 返回已发布文章总数。
         *
         * @return 文章总数
         */
        public long getTotalArticles() {
            return totalArticles;
        }

        /**
         * 返回有文章的作者数。
         *
         * @return 作者数
         */
        public long getTotalAuthors() {
            return totalAuthors;
        }

        /**
         * 返回已发布专栏总数。
         *
         * @return 专栏总数
         */
        public long getTotalColumns() {
            return totalColumns;
        }
    }
}
