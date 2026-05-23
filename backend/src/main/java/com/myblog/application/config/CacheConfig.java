package com.myblog.application.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.dto.NotificationDTO;
import com.myblog.application.dto.SearchBootstrapDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.dto.ArticleRecommendationsDTO;
import com.myblog.application.query.ArticlePageCacheKey;
import com.myblog.application.query.RecommendArticleCacheKey;
import com.myblog.application.service.HomeStatsAppService.HomeStats;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.growth.domain.model.valueobject.BadgeDefinition;
import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.shared.result.PageResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用缓存配置类，定义各类 Caffeine 本地缓存 Bean 及异步任务线程池。
 *
 * @author my-blog
 * @since 1.0.0
 */
@Configuration
public class CacheConfig {

    /**
     * 首页统计缓存。
     *
     * <p>统计值允许分钟级延迟，以换取首页高频访问下的聚合开销下降。</p>
     */
    @Bean
    public Cache<Long, HomeStats> homeStatsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(4, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 首页引导信息缓存。
     *
     * <p>该接口通常首屏就会命中，使用更短 TTL 兼顾刷新速度与接口吞吐。</p>
     */
    @Bean
    public Cache<String, HomeBootstrapDTO> homeBootstrapCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(45, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<String, List<CategoryDTO>> categoriesCache() {
        return Caffeine.newBuilder()
                .maximumSize(8)
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public Cache<String, List<TagDTO>> tagsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 榜单类缓存。
     *
     * <p>排行榜变化相对较慢，允许小时级缓存以减少重复排序计算。</p>
     */
    @Bean
    public Cache<String, List<ArticleDTO>> articleRankingsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<ArticleDTO>> featuredArticlesCache() {
        return Caffeine.newBuilder()
                .maximumSize(128)
                .expireAfterWrite(45, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<RecommendArticleCacheKey, PageResult<Article>> recommendedArticleFeedCache() {
        return Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<ArticlePageCacheKey, PageResult<Article>> publishedArticlePageCache() {
        return Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(45, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<Long, ArticleDTO> publicArticleDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<String, ArticleRecommendationsDTO> articleRecommendationsCache() {
        return Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public Cache<Long, java.util.Map<String, ArticleDTO>> articleNeighborsCache() {
        return Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public Cache<String, List<AuthorRankingDTO>> authorRankingsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
    }

    /**
     * 未读通知数缓存。
     *
     * <p>短 TTL 让角标尽快回落到真实值，避免用户长期看到过期未读数。</p>
     */
    @Bean
    public Cache<Long, Long> notificationUnreadCountCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<String, List<NotificationDTO>> recentNotificationsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<String, SearchBootstrapDTO> searchBootstrapCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public Cache<String, List<LevelThreshold>> levelThresholdsCache() {
        return Caffeine.newBuilder()
                .maximumSize(8)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<LevelRewardConfig>> levelRewardsCache() {
        return Caffeine.newBuilder()
                .maximumSize(16)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<LevelPrivilegeConfig>> levelPrivilegesCache() {
        return Caffeine.newBuilder()
                .maximumSize(32)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<GrowthRule>> growthRulesCache() {
        return Caffeine.newBuilder()
                .maximumSize(64)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<PointRule>> pointRulesCache() {
        return Caffeine.newBuilder()
                .maximumSize(64)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<ConsecutiveSignInRewardConfig>> consecutiveSignInRewardsCache() {
        return Caffeine.newBuilder()
                .maximumSize(16)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<CumulativeSignInRewardConfig>> cumulativeSignInRewardsCache() {
        return Caffeine.newBuilder()
                .maximumSize(16)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<String, List<BadgeDefinition>> badgeDefinitionsCache() {
        return Caffeine.newBuilder()
                .maximumSize(16)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public Cache<Integer, List<String>> hotKeywordsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 举报提交限流缓存。
     *
     * <p>以用户维度记录短时间内的提交次数，用于拦截突发刷举报行为。</p>
     */
    @Bean
    public Cache<Long, AtomicInteger> reportCreateRateCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 通用异步线程池。
     *
     * <p>队列满时回退到调用线程执行，优先保证任务不丢失；关闭应用时等待存量任务收尾。</p>
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        // 队列满时由调用线程执行，避免丢失任务
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // 应用关闭时等待正在执行的任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 成长模块异步线程池。
     *
     * <p>用于异步监听、分账补偿和批量补偿任务，避免与通用异步任务互相挤占。</p>
     */
    @Bean(name = "growthAsyncExecutor")
    public Executor growthAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("growth-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 统计计数异步线程池。
     *
     * <p>用于异步处理浏览、点赞、收藏、评论等计数更新。</p>
     */
    @Bean(name = "statsAsyncExecutor")
    public Executor statsAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("stats-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

}
