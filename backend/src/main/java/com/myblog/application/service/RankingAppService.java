package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ArticleSummaryDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class RankingAppService {

    private static final Logger log = LoggerFactory.getLogger(RankingAppService.class);
    private static final String PERIOD_7_DAYS = "7d";
    private static final String PERIOD_30_DAYS = "30d";
    private static final String PERIOD_ALL = "all";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final ArticleAssembler articleAssembler;
    private final Executor taskExecutor;
    private final Cache<String, List<ArticleDTO>> articleRankingsCache;
    private final Cache<String, List<AuthorRankingDTO>> authorRankingsCache;

    public RankingAppService(ArticleRepository articleRepository,
                             UserRepository userRepository,
                             UserFollowRepository userFollowRepository,
                             ArticleAssembler articleAssembler,
                             Executor taskExecutor,
                             @Qualifier("articleRankingsCache") Cache<String, List<ArticleDTO>> articleRankingsCache,
                             @Qualifier("authorRankingsCache")
                             Cache<String, List<AuthorRankingDTO>> authorRankingsCache) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.articleAssembler = articleAssembler;
        this.taskExecutor = taskExecutor;
        this.articleRankingsCache = articleRankingsCache;
        this.authorRankingsCache = authorRankingsCache;
    }

    public List<ArticleDTO> listArticleRankings(int limit) {
        return listArticleRankings(limit, PERIOD_ALL, null);
    }

    public List<ArticleDTO> listArticleRankings(int limit, String period, String category) {
        long _start = System.currentTimeMillis();
        int normalizedLimit = normalizeLimit(limit);
        String normalizedPeriod = normalizePeriod(period);
        String normalizedCategory = normalizeCategory(category);
        LocalDateTime publishedAfter = resolvePublishedAfter(normalizedPeriod);
        String cacheKey = buildCacheKey(normalizedLimit, normalizedPeriod, normalizedCategory);

        List<ArticleDTO> cached = articleRankingsCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("{} | 系统 查询文章排行榜 | 入参({}) | 结果({}) | {}",
                BizLogHelper.trace(),
                BizLogHelper.params(
                    "limit", normalizedLimit,
                    "period", normalizedPeriod,
                    "category", normalizedCategory),
                BizLogHelper.result("cached, size=" + cached.size()),
                BizLogHelper.elapsed(_start));
            return cached;
        }
        List<ArticleDTO> items = loadArticleRankingItems(normalizedLimit, normalizedPeriod, normalizedCategory);
        articleRankingsCache.put(cacheKey, items);
        log.info("{} | 系统 查询文章排行榜 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.params(
                "limit", normalizedLimit,
                "period", normalizedPeriod,
                "category", normalizedCategory),
            BizLogHelper.result("size=" + items.size()),
            BizLogHelper.elapsed(_start));
        return items;
    }

    private List<ArticleDTO> loadArticleRankingItems(int normalizedLimit,
                                                     String normalizedPeriod,
                                                     String normalizedCategory) {
        LocalDateTime publishedAfter = resolvePublishedAfter(normalizedPeriod);
        List<Article> articles =
            articleRepository.findRankingArticles(normalizedCategory, publishedAfter, normalizedLimit);
        if (articles.isEmpty() && publishedAfter != null) {
            articles = articleRepository.findRankingArticles(normalizedCategory, null, normalizedLimit);
        }

        if (articles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> authorIds = articles.stream()
            .map(a -> a.getAuthorId().getValue())
            .distinct()
            .collect(Collectors.toList());

        CompletableFuture<Map<Long, User>> authorFuture = CompletableFuture.supplyAsync(() ->
            userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u)), taskExecutor);

        Map<Long, User> authorMap = authorFuture.join();

        List<ArticleDTO> items = new ArrayList<>(articles.size());
        for (Article article : articles) {
            User author = authorMap.get(article.getAuthorId().getValue());
            if (author != null) {
                items.add(articleAssembler.toDTO(article, author));
            }
        }
        return items;
    }

    public List<AuthorRankingDTO> listAuthorRankings(int limit, Long currentUserId) {
        return listAuthorRankings(limit, PERIOD_ALL, null, currentUserId);
    }

    public List<AuthorRankingDTO> listAuthorRankings(int limit, String period, String category, Long currentUserId) {
        long _start = System.currentTimeMillis();
        int normalizedLimit = normalizeLimit(limit);
        String normalizedPeriod = normalizePeriod(period);
        String normalizedCategory = normalizeCategory(category);
        LocalDateTime publishedAfter = resolvePublishedAfter(normalizedPeriod);
        String cacheKey = buildCacheKey(normalizedLimit, normalizedPeriod, normalizedCategory);

        List<AuthorRankingDTO> cachedBase = authorRankingsCache.getIfPresent(cacheKey);
        if (cachedBase != null) {
            List<AuthorRankingDTO> result = applyFollowStatus(copyAuthorRankings(cachedBase), currentUserId);
            log.info("{} | {} 查询作者排行榜 | 入参({}) | 结果({}) | {}",
                BizLogHelper.trace(),
                BizLogHelper.who(currentUserId),
                BizLogHelper.params(
                    "limit", normalizedLimit,
                    "period", normalizedPeriod,
                    "category", normalizedCategory),
                BizLogHelper.result("cached, size=" + result.size()),
                BizLogHelper.elapsed(_start));
            return result;
        }
        List<AuthorRankingDTO> baseItems =
            loadBaseAuthorRankingItems(normalizedLimit, normalizedPeriod, normalizedCategory);
        authorRankingsCache.put(cacheKey, copyAuthorRankings(baseItems));
        List<AuthorRankingDTO> finalResult = applyFollowStatus(copyAuthorRankings(baseItems), currentUserId);
        log.info("{} | {} 查询作者排行榜 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(currentUserId),
            BizLogHelper.params(
                "limit", normalizedLimit,
                "period", normalizedPeriod,
                "category", normalizedCategory),
            BizLogHelper.result("size=" + finalResult.size()),
            BizLogHelper.elapsed(_start));
        return finalResult;
    }

    private List<AuthorRankingDTO> loadBaseAuthorRankingItems(int normalizedLimit,
                                                              String normalizedPeriod,
                                                              String normalizedCategory) {
        LocalDateTime publishedAfter = resolvePublishedAfter(normalizedPeriod);
        List<AuthorArticleStatsDO> statsList =
            articleRepository.findAuthorArticleStats(normalizedLimit, normalizedCategory, publishedAfter);
        LocalDateTime topArticlePublishedAfter = publishedAfter;
        if (statsList.isEmpty() && publishedAfter != null) {
            statsList = articleRepository.findAuthorArticleStats(normalizedLimit, normalizedCategory, null);
            topArticlePublishedAfter = null;
        }
        final LocalDateTime topArticleAfter = topArticlePublishedAfter;

        if (statsList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> authorIds = statsList.stream()
            .map(AuthorArticleStatsDO::getAuthorId)
            .collect(Collectors.toList());

        CompletableFuture<Map<Long, User>> authorFuture = CompletableFuture.supplyAsync(() ->
            userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u)), taskExecutor);

        CompletableFuture<Map<Long, Integer>> followerCountFuture = CompletableFuture.supplyAsync(() ->
            userFollowRepository.countFollowersBatch(authorIds), taskExecutor);

        CompletableFuture<Map<Long, ArticleSummaryDTO>> topArticleFuture = CompletableFuture.supplyAsync(() ->
            buildTopArticleMap(authorIds, normalizedCategory, topArticleAfter), taskExecutor);

        Map<Long, User> authorMap = authorFuture.join();
        Map<Long, Integer> followerCountMap = followerCountFuture.join();
        Map<Long, ArticleSummaryDTO> topArticleMap = topArticleFuture.join();

        List<AuthorRankingDTO> result = new ArrayList<>(statsList.size());
        int rank = 1;
        for (AuthorArticleStatsDO stats : statsList) {
            User author = authorMap.get(stats.getAuthorId());
            if (author == null) {
                continue;
            }
            AuthorRankingDTO dto = new AuthorRankingDTO();
            dto.setUser(UserAssembler.toDTO(author));
            dto.setArticleCount(stats.getArticleCount() != null ? stats.getArticleCount() : 0);
            dto.setTotalViewCount(stats.getTotalViews() != null ? stats.getTotalViews() : 0L);
            dto.setTotalLikeCount(stats.getTotalLikes() != null ? stats.getTotalLikes() : 0L);
            dto.setFollowerCount(followerCountMap.getOrDefault(stats.getAuthorId(), 0));
            dto.setFollowed(false);
            dto.setTopArticle(topArticleMap.get(stats.getAuthorId()));
            dto.setRank(rank++);
            result.add(dto);
        }
        return result;
    }

    @Scheduled(cron = "${my-blog.ranking.refresh-cron:0 0 * * * *}")
    public void refreshRankingCaches() {
        long _start = System.currentTimeMillis();
        List<RankingQuery> articleQueries = buildRefreshQueries(articleRankingsCache);
        List<RankingQuery> authorQueries = buildRefreshQueries(authorRankingsCache);
        int refreshedArticleCount = 0;
        int refreshedAuthorCount = 0;
        for (RankingQuery query : articleQueries) {
            try {
                articleRankingsCache.put(
                    query.cacheKey,
                    loadArticleRankingItems(query.limit, query.period, query.category)
                );
                refreshedArticleCount++;
            } catch (Exception ex) {
                log.warn("{} | 系统 刷新文章排行榜缓存失败 | 入参({}) | {}",
                    BizLogHelper.trace(),
                    BizLogHelper.params(
                        "limit", query.limit,
                        "period", query.period,
                        "category", query.category),
                    BizLogHelper.elapsed(_start),
                    ex);
            }
        }
        for (RankingQuery query : authorQueries) {
            try {
                authorRankingsCache.put(
                    query.cacheKey,
                    copyAuthorRankings(loadBaseAuthorRankingItems(query.limit, query.period, query.category))
                );
                refreshedAuthorCount++;
            } catch (Exception ex) {
                log.warn("{} | 系统 刷新作者排行榜缓存失败 | 入参({}) | {}",
                    BizLogHelper.trace(),
                    BizLogHelper.params(
                        "limit", query.limit,
                        "period", query.period,
                        "category", query.category),
                    BizLogHelper.elapsed(_start),
                    ex);
            }
        }
        log.info("{} | 系统 定时刷新排行榜缓存 | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.result(
                "articleKeys=" + refreshedArticleCount + ", authorKeys=" + refreshedAuthorCount),
            BizLogHelper.elapsed(_start));
    }

    private List<RankingQuery> buildRefreshQueries(Cache<String, ?> cache) {
        Map<String, RankingQuery> queryMap = new LinkedHashMap<String, RankingQuery>();
        for (String period : Arrays.asList(PERIOD_7_DAYS, PERIOD_30_DAYS, PERIOD_ALL)) {
            putRefreshQuery(queryMap, DEFAULT_LIMIT, period, null);
        }
        for (String cacheKey : cache.asMap().keySet()) {
            RankingQuery query = parseCacheKey(cacheKey);
            if (query != null) {
                queryMap.put(query.cacheKey, query);
            }
        }
        return new ArrayList<RankingQuery>(queryMap.values());
    }

    private void putRefreshQuery(Map<String, RankingQuery> queryMap, int limit, String period, String category) {
        RankingQuery query = new RankingQuery(normalizeLimit(limit), normalizePeriod(period), normalizeCategory(category));
        queryMap.put(query.cacheKey, query);
    }

    private RankingQuery parseCacheKey(String cacheKey) {
        if (cacheKey == null) {
            return null;
        }
        String[] parts = cacheKey.split(":", 3);
        if (parts.length < 2) {
            return null;
        }
        try {
            int limit = Integer.parseInt(parts[0]);
            String period = parts[1];
            String category = parts.length > 2 ? parts[2] : null;
            return new RankingQuery(normalizeLimit(limit), normalizePeriod(period), normalizeCategory(category));
        } catch (NumberFormatException ex) {
            log.warn("{} | 系统 解析排行榜缓存 key 失败 | 入参({})",
                BizLogHelper.trace(),
                BizLogHelper.params("cacheKey", cacheKey),
                ex);
            return null;
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizePeriod(String period) {
        String value = period == null ? "" : period.trim().toLowerCase();
        if (PERIOD_30_DAYS.equals(value) || PERIOD_ALL.equals(value)) {
            return value;
        }
        return PERIOD_7_DAYS;
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String value = category.trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDateTime resolvePublishedAfter(String period) {
        if (PERIOD_ALL.equals(period)) {
            return null;
        }
        if (PERIOD_30_DAYS.equals(period)) {
            return LocalDateTime.now().minusDays(30);
        }
        return LocalDateTime.now().minusDays(7);
    }

    private String buildCacheKey(int limit, String period, String category) {
        return limit + ":" + period + ":" + (category == null ? "" : category);
    }

    private Map<Long, ArticleSummaryDTO> buildTopArticleMap(List<Long> authorIds,
                                                            String category,
                                                            LocalDateTime publishedAfter) {
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Article> articles =
            articleRepository.findTopRankingArticlesByAuthorIds(authorIds, category, publishedAfter);
        Map<Long, ArticleSummaryDTO> result = new HashMap<Long, ArticleSummaryDTO>();
        for (Article article : articles) {
            Long authorId = article.getAuthorId().getValue();
            if (!result.containsKey(authorId)) {
                result.put(authorId, toArticleSummary(article));
            }
        }
        return result;
    }

    private ArticleSummaryDTO toArticleSummary(Article article) {
        ArticleSummaryDTO dto = new ArticleSummaryDTO();
        dto.setId(article.getId().getValue());
        dto.setTitle(article.getTitle());
        dto.setSlug(article.getSlug());
        return dto;
    }

    private Map<Long, Boolean> buildFollowStatusMap(Long currentUserId, List<Long> authorIds) {
        if (currentUserId == null || authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> followedAuthorIds =
            userFollowRepository.findFollowingUserIdsIn(new UserId(currentUserId), authorIds);
        Map<Long, Boolean> result = new HashMap<Long, Boolean>();
        for (Long authorId : authorIds) {
            result.put(authorId, false);
        }
        for (Long authorId : followedAuthorIds) {
            result.put(authorId, true);
        }
        return result;
    }

    private List<AuthorRankingDTO> applyFollowStatus(List<AuthorRankingDTO> items, Long currentUserId) {
        if (items.isEmpty()) {
            return items;
        }
        List<Long> authorIds = items.stream()
                .map(item -> item.getUser().getId())
                .collect(Collectors.toList());
        Map<Long, Boolean> followStatusMap = buildFollowStatusMap(currentUserId, authorIds);
        for (AuthorRankingDTO item : items) {
            item.setFollowed(followStatusMap.getOrDefault(item.getUser().getId(), false));
        }
        return items;
    }

    private List<AuthorRankingDTO> copyAuthorRankings(List<AuthorRankingDTO> source) {
        List<AuthorRankingDTO> copies = new ArrayList<AuthorRankingDTO>(source.size());
        for (AuthorRankingDTO item : source) {
            AuthorRankingDTO dto = new AuthorRankingDTO();
            dto.setRank(item.getRank());
            dto.setUser(copyUser(item.getUser()));
            dto.setArticleCount(item.getArticleCount());
            dto.setTotalViewCount(item.getTotalViewCount());
            dto.setTotalLikeCount(item.getTotalLikeCount());
            dto.setFollowerCount(item.getFollowerCount());
            dto.setFollowed(item.isFollowed());
            dto.setTopArticle(copyArticleSummary(item.getTopArticle()));
            copies.add(dto);
        }
        return copies;
    }

    private ArticleSummaryDTO copyArticleSummary(ArticleSummaryDTO source) {
        if (source == null) {
            return null;
        }
        ArticleSummaryDTO dto = new ArticleSummaryDTO();
        dto.setId(source.getId());
        dto.setTitle(source.getTitle());
        dto.setSlug(source.getSlug());
        return dto;
    }

    private static class RankingQuery {
        private final int limit;
        private final String period;
        private final String category;
        private final String cacheKey;

        RankingQuery(int limit, String period, String category) {
            this.limit = limit;
            this.period = period;
            this.category = category;
            this.cacheKey = limit + ":" + period + ":" + (category == null ? "" : category);
        }
    }

    private UserDTO copyUser(UserDTO source) {
        UserDTO dto = new UserDTO();
        dto.setId(source.getId());
        dto.setUsername(source.getUsername());
        dto.setEmail(source.getEmail());
        dto.setNickname(source.getNickname());
        dto.setAvatarUrl(source.getAvatarUrl());
        dto.setBio(source.getBio());
        dto.setRole(source.getRole());
        return dto;
    }
}
