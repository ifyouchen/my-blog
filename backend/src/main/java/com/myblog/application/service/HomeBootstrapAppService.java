package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页启动数据应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class HomeBootstrapAppService {

    private static final String BOOTSTRAP_CACHE_KEY = "home-bootstrap";

    private final Cache<String, HomeBootstrapDTO> homeBootstrapCache;
    private final HomeStatsAppService homeStatsAppService;
    private final CategoryAppService categoryAppService;
    private final ColumnAppService columnAppService;
    private final RankingAppService rankingAppService;

    public HomeBootstrapAppService(@Qualifier("homeBootstrapCache") Cache<String, HomeBootstrapDTO> homeBootstrapCache,
                                   HomeStatsAppService homeStatsAppService,
                                   CategoryAppService categoryAppService,
                                   ColumnAppService columnAppService,
                                   RankingAppService rankingAppService) {
        this.homeBootstrapCache = homeBootstrapCache;
        this.homeStatsAppService = homeStatsAppService;
        this.categoryAppService = categoryAppService;
        this.columnAppService = columnAppService;
        this.rankingAppService = rankingAppService;
    }

    public HomeBootstrapDTO getBootstrap() {
        HomeBootstrapDTO cached = homeBootstrapCache.getIfPresent(BOOTSTRAP_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        HomeBootstrapDTO bootstrap = new HomeBootstrapDTO();
        bootstrap.setStats(homeStatsAppService.getStats());
        bootstrap.setCategories(categoryAppService.getCategories(true));
        bootstrap.setRecommendedColumns(columnAppService.listRecommendedColumns(3, null));
        bootstrap.setAuthorRankings(normalizeAuthorRankings(rankingAppService.listAuthorRankings(3, null)));
        homeBootstrapCache.put(BOOTSTRAP_CACHE_KEY, bootstrap);
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
