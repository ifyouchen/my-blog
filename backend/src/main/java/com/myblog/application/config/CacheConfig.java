package com.myblog.application.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.application.cache.TwoTierCache;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ArticleRecommendationsDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.dto.NotificationDTO;
import com.myblog.application.dto.SearchBootstrapDTO;
import com.myblog.application.dto.TagDTO;
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
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用缓存配置类，定义两级缓存（Caffeine L1 + Redisson L2）及异步任务线程池。
 *
 * @author my-blog
 * @since 1.0.0
 */
@Configuration
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    public CacheConfig(RedissonClient redissonClient, StringRedisTemplate redisTemplate) {
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void clearOldCacheData() {
        Set<String> keys = redisTemplate.keys("twoTierCache:*");
        if (keys != null && !keys.isEmpty()) {
            log.info("Clearing {} stale cache keys from Redis (format change)", keys.size());
            redisTemplate.delete(keys);
        }
    }

    private static long l1Ttl(long l2Seconds) {
        return Math.max(l2Seconds * 100, 5000L);
    }

    @Bean
    public Cache<Long, HomeStats> homeStatsCache() {
        long l2 = TimeUnit.MINUTES.toSeconds(4);
        return new TwoTierCache<>("homeStats", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, HomeBootstrapDTO> homeBootstrapCache() {
        long l2 = TimeUnit.SECONDS.toSeconds(45);
        return new TwoTierCache<>("homeBootstrap", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<CategoryDTO>> categoriesCache() {
        long l2 = TimeUnit.MINUTES.toSeconds(3);
        return new TwoTierCache<>("categories", l1Ttl(l2), l2, 8, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<TagDTO>> tagsCache() {
        long l2 = TimeUnit.MINUTES.toSeconds(3);
        return new TwoTierCache<>("tags", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<ArticleDTO>> articleRankingsCache() {
        long l2 = TimeUnit.HOURS.toSeconds(2);
        return new TwoTierCache<>("articleRankings", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<ArticleDTO>> featuredArticlesCache() {
        long l2 = TimeUnit.SECONDS.toSeconds(45);
        return new TwoTierCache<>("featuredArticles", l1Ttl(l2), l2, 128, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<RecommendArticleCacheKey, PageResult<Article>> recommendedArticleFeedCache() {
        long l2 = TimeUnit.HOURS.toSeconds(1);
        return new TwoTierCache<>("recommendedArticleFeed", l1Ttl(l2), l2, 512, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<ArticlePageCacheKey, PageResult<Article>> publishedArticlePageCache() {
        long l2 = TimeUnit.SECONDS.toSeconds(45);
        return new TwoTierCache<>("publishedArticlePage", l1Ttl(l2), l2, 512, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<Long, ArticleDTO> publicArticleDetailCache() {
        long l2 = TimeUnit.SECONDS.toSeconds(60);
        return new TwoTierCache<>("publicArticleDetail", l1Ttl(l2), l2, 512, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, ArticleRecommendationsDTO> articleRecommendationsCache() {
        long l2 = TimeUnit.MINUTES.toSeconds(2);
        return new TwoTierCache<>("articleRecommendations", l1Ttl(l2), l2, 512, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<Long, Map<String, ArticleDTO>> articleNeighborsCache() {
        long l2 = TimeUnit.MINUTES.toSeconds(2);
        return new TwoTierCache<>("articleNeighbors", l1Ttl(l2), l2, 512, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<AuthorRankingDTO>> authorRankingsCache() {
        long l2 = TimeUnit.HOURS.toSeconds(2);
        return new TwoTierCache<>("authorRankings", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<Long, Long> notificationUnreadCountCache() {
        long l2 = TimeUnit.SECONDS.toSeconds(30);
        return new TwoTierCache<>("notificationUnreadCount", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<NotificationDTO>> recentNotificationsCache() {
        long l2 = TimeUnit.SECONDS.toSeconds(30);
        return new TwoTierCache<>("recentNotifications", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, SearchBootstrapDTO> searchBootstrapCache() {
        long l2 = TimeUnit.MINUTES.toSeconds(3);
        return new TwoTierCache<>("searchBootstrap", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<LevelThreshold>> levelThresholdsCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("levelThresholds", l1Ttl(l2), l2, 8, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<LevelRewardConfig>> levelRewardsCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("levelRewards", l1Ttl(l2), l2, 16, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<LevelPrivilegeConfig>> levelPrivilegesCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("levelPrivileges", l1Ttl(l2), l2, 32, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<GrowthRule>> growthRulesCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("growthRules", l1Ttl(l2), l2, 64, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<PointRule>> pointRulesCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("pointRules", l1Ttl(l2), l2, 64, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<ConsecutiveSignInRewardConfig>> consecutiveSignInRewardsCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("consecutiveSignInRewards", l1Ttl(l2), l2, 16, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<CumulativeSignInRewardConfig>> cumulativeSignInRewardsCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("cumulativeSignInRewards", l1Ttl(l2), l2, 16, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<String, List<BadgeDefinition>> badgeDefinitionsCache() {
        long l2 = TimeUnit.HOURS.toSeconds(12);
        return new TwoTierCache<>("badgeDefinitions", l1Ttl(l2), l2, 16, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<Integer, List<String>> hotKeywordsCache() {
        long l2 = TimeUnit.MINUTES.toSeconds(5);
        return new TwoTierCache<>("hotKeywords", l1Ttl(l2), l2, 0, redissonClient, redisTemplate);
    }

    @Bean
    public Cache<Long, AtomicInteger> reportCreateRateCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

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
