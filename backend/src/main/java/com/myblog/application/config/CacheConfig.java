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
import com.myblog.application.query.RecommendArticleCacheKey;
import com.myblog.application.service.HomeStatsAppService.HomeStats;
import com.myblog.domain.model.aggregate.Article;
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

    @Bean
    public Cache<Long, HomeStats> homeStatsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(4, TimeUnit.MINUTES)
                .build();
    }

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
    public Cache<String, List<AuthorRankingDTO>> authorRankingsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
    }

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
    public Cache<Integer, List<String>> hotKeywordsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
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
        // 队列满时由调用线程执行，避免丢失任务
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // 应用关闭时等待正在执行的任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

}
