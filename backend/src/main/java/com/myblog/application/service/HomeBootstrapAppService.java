package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.service.HomeStatsAppService.HomeStats;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 首页启动数据应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class HomeBootstrapAppService {

    static final String BOOTSTRAP_CACHE_KEY = "home-bootstrap";

    private final Cache<String, HomeBootstrapDTO> homeBootstrapCache;
    private final HomeStatsAppService homeStatsAppService;
    private final CategoryAppService categoryAppService;
    private final ColumnAppService columnAppService;
    private final RankingAppService rankingAppService;
    private final RecommendationAppService recommendationAppService;
    private final TopicAppService topicAppService;
    private final TagAppService tagAppService;
    private final SearchHistoryAppService searchHistoryAppService;
    private final Executor taskExecutor;

    public HomeBootstrapAppService(@Qualifier("homeBootstrapCache") Cache<String, HomeBootstrapDTO> homeBootstrapCache,
                                   HomeStatsAppService homeStatsAppService,
                                   CategoryAppService categoryAppService,
                                   ColumnAppService columnAppService,
                                   RankingAppService rankingAppService,
                                   RecommendationAppService recommendationAppService,
                                   TopicAppService topicAppService,
                                   TagAppService tagAppService,
                                   SearchHistoryAppService searchHistoryAppService,
                                   Executor taskExecutor) {
        this.homeBootstrapCache = homeBootstrapCache;
        this.homeStatsAppService = homeStatsAppService;
        this.categoryAppService = categoryAppService;
        this.columnAppService = columnAppService;
        this.rankingAppService = rankingAppService;
        this.recommendationAppService = recommendationAppService;
        this.topicAppService = topicAppService;
        this.tagAppService = tagAppService;
        this.searchHistoryAppService = searchHistoryAppService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 获取首页引导数据（并行加载各模块数据，减少整体响应时间）。
     *
     * @return 首页引导数据
     */
    public HomeBootstrapDTO getBootstrap() {
        return homeBootstrapCache.get(BOOTSTRAP_CACHE_KEY, key -> loadBootstrap());
    }

    private HomeBootstrapDTO loadBootstrap() {
        // 并行加载 6 路数据，总耗时取决于最慢的那一路
        CompletableFuture<HomeStats> statsFuture =
            CompletableFuture.supplyAsync(() -> homeStatsAppService.getStats(), taskExecutor);

        CompletableFuture<List<CategoryDTO>> categoriesFuture =
            CompletableFuture.supplyAsync(() -> categoryAppService.getCategories(true), taskExecutor);

        CompletableFuture<List<ColumnDTO>> columnsFuture =
            CompletableFuture.supplyAsync(() -> columnAppService.listRecommendedColumns(3, null), taskExecutor);

        CompletableFuture<List<AuthorRankingDTO>> rankingsFuture =
            CompletableFuture.supplyAsync(() -> rankingAppService.listAuthorRankings(3, null), taskExecutor);

        CompletableFuture<List<ArticleDTO>> featuredFuture =
            CompletableFuture.supplyAsync(() -> recommendationAppService.listFeaturedArticles(1, 5), taskExecutor);

        CompletableFuture<List<TopicDTO>> hotTopicsFuture =
            CompletableFuture.supplyAsync(() -> topicAppService.listHotTopics(6), taskExecutor);

        CompletableFuture<List<TagDTO>> hotTagsFuture =
            CompletableFuture.supplyAsync(() -> tagAppService.getHotTags(12), taskExecutor);

        CompletableFuture<List<String>> hotKeywordsFuture =
            CompletableFuture.supplyAsync(() -> searchHistoryAppService.getHotKeywords(10), taskExecutor);

        // 等待所有完成并组装结果
        List<ArticleDTO> featuredArticles = featuredFuture.join();
        List<TopicDTO> hotTopics = hotTopicsFuture.join();
        HomeBootstrapDTO bootstrap = new HomeBootstrapDTO();
        bootstrap.setStats(statsFuture.join());
        bootstrap.setCategories(categoriesFuture.join());
        bootstrap.setRecommendedColumns(columnsFuture.join());
        bootstrap.setAuthorRankings(normalizeAuthorRankings(rankingsFuture.join()));
        bootstrap.setFeaturedArticles(featuredArticles);
        bootstrap.setTodayFocus(featuredArticles.isEmpty() ? null : featuredArticles.get(0));
        bootstrap.setHotTopics(hotTopics);
        bootstrap.setLearningTopics(hotTopics);
        bootstrap.setHotTags(hotTagsFuture.join());
        bootstrap.setHotKeywords(hotKeywordsFuture.join());
        return bootstrap;
    }

    public void evict() {
        homeBootstrapCache.invalidate(BOOTSTRAP_CACHE_KEY);
    }

    private List<AuthorRankingDTO> normalizeAuthorRankings(List<AuthorRankingDTO> items) {
        for (AuthorRankingDTO item : items) {
            item.setFollowed(false);
        }
        return items;
    }
}
