package com.myblog.application.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.service.HomeStatsAppService.HomeStats;
import com.myblog.application.dto.NotificationDTO;
import com.myblog.application.dto.SearchBootstrapDTO;
import com.myblog.infrastructure.config.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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
    public Cache<Integer, List<ArticleDTO>> articleRankingsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(45, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<Integer, List<AuthorRankingDTO>> authorRankingsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(45, TimeUnit.SECONDS)
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
     * Snowflake ID 生成器，用于替换所有表的 MAX(id)+1 模式。
     * machineId 默认为 1，可通过环境变量 SNOWFLAKE_MACHINE_ID 配置（0-1023）。
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${my-blog.snowflake.machine-id:1}") long machineId) {
        return new SnowflakeIdGenerator(machineId);
    }
}
