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
import com.myblog.domain.model.readmodel.AuthorArticleStats;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 排行榜应用服务。
 * <p>
 * 提供文章排行榜和作者排行榜查询功能，支持按时间段（7天/30天/全部）和分类过滤。
 * 内置 Caffeine 缓存，并通过定时任务每小时刷新缓存数据。
 * </p>
 *
 * @author Codex
 * @since 1.0.0
 */
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
    private final UserLevelAppService userLevelAppService;
    private final Executor taskExecutor;
    private final Cache<String, List<ArticleDTO>> articleRankingsCache;
    private final Cache<String, List<AuthorRankingDTO>> authorRankingsCache;

    public RankingAppService(ArticleRepository articleRepository,
                             UserRepository userRepository,
                             UserFollowRepository userFollowRepository,
                             ArticleAssembler articleAssembler,
                             UserLevelAppService userLevelAppService,
                             Executor taskExecutor,
                             @Qualifier("articleRankingsCache") Cache<String, List<ArticleDTO>> articleRankingsCache,
                             @Qualifier("authorRankingsCache")
                             Cache<String, List<AuthorRankingDTO>> authorRankingsCache) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.articleAssembler = articleAssembler;
        this.userLevelAppService = userLevelAppService;
        this.taskExecutor = taskExecutor;
        this.articleRankingsCache = articleRankingsCache;
        this.authorRankingsCache = authorRankingsCache;
    }

    /**
     * 查询文章排行榜（使用全部时间段，无分类过滤）。
     *
     * @param limit 返回数量
     * @return 文章排行榜列表
     */
    public List<ArticleDTO> listArticleRankings(int limit) {
        return listArticleRankings(limit, PERIOD_ALL, null);
    }

    /**
     * 查询文章排行榜（支持时间段和分类过滤）。
     *
     * @param limit    返回数量
     * @param period   时间段（7d / 30d / all）
     * @param category 分类筛选（null 表示不限）
     * @return 文章排行榜列表
     */
    public List<ArticleDTO> listArticleRankings(int limit, String period, String category) {
        long _start = System.currentTimeMillis();
        int normalizedLimit = normalizeLimit(limit);
        String normalizedPeriod = normalizePeriod(period);
        String normalizedCategory = normalizeCategory(category);
        String cacheKey = buildCacheKey(normalizedLimit, normalizedPeriod, normalizedCategory);

        // Only cache "all" period; 7d/30d have sliding time window that would return stale results
        if (PERIOD_ALL.equals(normalizedPeriod)) {
            List<ArticleDTO> cached = articleRankingsCache.getIfPresent(cacheKey);
            if (cached != null) {
                userLevelAppService.fillLevels(cached.stream()
                    .map(ArticleDTO::getAuthor)
                    .collect(Collectors.toList()));
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
        }
        List<ArticleDTO> items = loadArticleRankingItems(normalizedLimit, normalizedPeriod, normalizedCategory);
        if (PERIOD_ALL.equals(normalizedPeriod)) {
            articleRankingsCache.put(cacheKey, items);
        }
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

    /**
     * 从仓储层加载文章排行条目，若指定时段无数据则自动回退到全时段。
     *
     * @param normalizedLimit    已规范化的返回数量
     * @param normalizedPeriod   已规范化的时间段
     * @param normalizedCategory 已规范化的分类
     * @return 文章 DTO 列表
     */
    private List<ArticleDTO> loadArticleRankingItems(int normalizedLimit,
                                                     String normalizedPeriod,
                                                     String normalizedCategory) {
        LocalDateTime publishedAfter = resolvePublishedAfter(normalizedPeriod);
        List<Article> articles =
            articleRepository.findRankingArticles(normalizedCategory, publishedAfter, normalizedLimit);

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
        userLevelAppService.fillLevels(items.stream()
            .map(ArticleDTO::getAuthor)
            .collect(Collectors.toList()));
        return items;
    }

    /**
     * 查询作者排行榜（使用全部时间段，无分类过滤）。
     *
     * @param limit         返回数量
     * @param currentUserId 当前用户 ID（用于填充关注状态，未登录时为 null）
     * @return 作者排行榜列表
     */
    public List<AuthorRankingDTO> listAuthorRankings(int limit, Long currentUserId) {
        return listAuthorRankings(limit, PERIOD_ALL, null, currentUserId);
    }

    /**
     * 查询作者排行榜（支持时间段和分类过滤）。
     *
     * @param limit         返回数量
     * @param period        时间段（7d / 30d / all）
     * @param category      分类筛选（null 表示不限）
     * @param currentUserId 当前用户 ID（用于填充关注状态，未登录时为 null）
     * @return 作者排行榜列表
     */
    public List<AuthorRankingDTO> listAuthorRankings(int limit, String period, String category, Long currentUserId) {
        long _start = System.currentTimeMillis();
        int normalizedLimit = normalizeLimit(limit);
        String normalizedPeriod = normalizePeriod(period);
        String normalizedCategory = normalizeCategory(category);
        String cacheKey = buildCacheKey(normalizedLimit, normalizedPeriod, normalizedCategory);

        // Only cache "all" period; 7d/30d have sliding time window that would return stale results
        if (PERIOD_ALL.equals(normalizedPeriod)) {
            List<AuthorRankingDTO> cachedBase = authorRankingsCache.getIfPresent(cacheKey);
            if (cachedBase != null) {
                List<AuthorRankingDTO> result = applyFollowStatus(copyAuthorRankings(cachedBase), currentUserId);
                userLevelAppService.fillLevels(result.stream()
                    .map(AuthorRankingDTO::getUser)
                    .collect(Collectors.toList()));
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
        }
        List<AuthorRankingDTO> baseItems =
            loadBaseAuthorRankingItems(normalizedLimit, normalizedPeriod, normalizedCategory);
        if (PERIOD_ALL.equals(normalizedPeriod)) {
            authorRankingsCache.put(cacheKey, copyAuthorRankings(baseItems));
        }
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

    /**
     * 从仓储层加载作者排行基础条目（不含关注状态），若指定时段无数据则自动回退全时段。
     *
     * @param normalizedLimit    已规范化的返回数量
     * @param normalizedPeriod   已规范化的时间段
     * @param normalizedCategory 已规范化的分类
     * @return 作者排行 DTO 列表
     */
    private List<AuthorRankingDTO> loadBaseAuthorRankingItems(int normalizedLimit,
                                                              String normalizedPeriod,
                                                              String normalizedCategory) {
        LocalDateTime publishedAfter = resolvePublishedAfter(normalizedPeriod);
        List<AuthorArticleStats> statsList =
            articleRepository.findAuthorArticleStats(normalizedLimit, normalizedCategory, publishedAfter);

        if (statsList.isEmpty()) {
            return new ArrayList<>();
        }

        final LocalDateTime topArticleAfter = publishedAfter;

        List<Long> authorIds = statsList.stream()
            .map(AuthorArticleStats::getAuthorId)
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
        for (AuthorArticleStats stats : statsList) {
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
        userLevelAppService.fillLevels(result.stream()
            .map(AuthorRankingDTO::getUser)
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 定时刷新排行榜缓存（默认每小时一次）。
     */
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

    /**
     * 根据缓存中现有的 key 构建需要刷新的查询列表，同时补充默认查询项。
     *
     * @param cache 目标缓存
     * @return 需要刷新的查询列表
     */
    private List<RankingQuery> buildRefreshQueries(Cache<String, ?> cache) {
        Map<String, RankingQuery> queryMap = new LinkedHashMap<String, RankingQuery>();
        putRefreshQuery(queryMap, DEFAULT_LIMIT, PERIOD_ALL, null);
        for (String cacheKey : cache.asMap().keySet()) {
            RankingQuery query = parseCacheKey(cacheKey);
            if (query != null && PERIOD_ALL.equals(query.period)) {
                queryMap.put(query.cacheKey, query);
            }
        }
        return new ArrayList<RankingQuery>(queryMap.values());
    }

    /**
     * 将刷新查询项放入查询 Map（key 为缓存键，避免重复）。
     *
     * @param queryMap 查询 Map
     * @param limit    返回数量
     * @param period   时间段
     * @param category 分类
     */
    private void putRefreshQuery(Map<String, RankingQuery> queryMap, int limit, String period, String category) {
        RankingQuery query = new RankingQuery(
            normalizeLimit(limit),
            normalizePeriod(period),
            normalizeCategory(category)
        );
        queryMap.put(query.cacheKey, query);
    }

    /**
     * 解析缓存 key 为 RankingQuery 对象，解析失败时返回 null。
     *
     * @param cacheKey 缓存键字符串
     * @return 解析后的 RankingQuery，解析失败时返回 null
     */
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

    /**
     * 规范化返回数量，小于等于 0 时使用默认值，超过最大值时截断。
     *
     * @param limit 原始限制数量
     * @return 规范化后的限制数量
     */
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 规范化时间段，无效值时回退到 7d。
     *
     * @param period 原始时间段字符串
     * @return 规范化后的时间段
     */
    private String normalizePeriod(String period) {
        String value = period == null ? "" : period.trim().toLowerCase();
        if (PERIOD_30_DAYS.equals(value) || PERIOD_ALL.equals(value)) {
            return value;
        }
        return PERIOD_7_DAYS;
    }

    /**
     * 规范化分类，空字符串转为 null。
     *
     * @param category 原始分类字符串
     * @return 规范化后的分类，无效时返回 null
     */
    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String value = category.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 根据时间段计算发布时间下限，全部时间段返回 null。
     *
     * @param period 时间段字符串
     * @return 发布时间下限，全部时段时为 null
     */
    private LocalDateTime resolvePublishedAfter(String period) {
        if (PERIOD_ALL.equals(period)) {
            return null;
        }
        if (PERIOD_30_DAYS.equals(period)) {
            return LocalDateTime.now().minusDays(30);
        }
        return LocalDateTime.now().minusDays(7);
    }

    /**
     * 构建缓存键。
     *
     * @param limit    返回数量
     * @param period   时间段
     * @param category 分类
     * @return 缓存键字符串
     */
    private String buildCacheKey(int limit, String period, String category) {
        return limit + ":" + period + ":" + (category == null ? "" : category);
    }

    /**
     * 构建作者 ID 到其最热文章 DTO 的映射（每个作者只取一篇）。
     *
     * @param authorIds      作者 ID 列表
     * @param category       分类筛选
     * @param publishedAfter 发布时间下限
     * @return 作者 ID 到热门文章摘要 DTO 的映射
     */
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

    /**
     * 将文章领域对象转换为摘要 DTO（仅含 ID、标题、Slug）。
     *
     * @param article 文章领域对象
     * @return 文章摘要 DTO
     */
    private ArticleSummaryDTO toArticleSummary(Article article) {
        ArticleSummaryDTO dto = new ArticleSummaryDTO();
        dto.setId(article.getId().getValue());
        dto.setTitle(article.getTitle());
        dto.setSlug(article.getSlug());
        return dto;
    }

    /**
     * 构建当前用户对指定作者列表的关注状态 Map。
     *
     * @param currentUserId 当前用户 ID
     * @param authorIds     作者 ID 列表
     * @return 作者 ID 到关注状态的映射
     */
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

    /**
     * 将关注状态填充到作者排行列表中。
     *
     * @param items         作者排行 DTO 列表
     * @param currentUserId 当前用户 ID（未登录时为 null）
     * @return 填充关注状态后的列表
     */
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

    /**
     * 深拷贝作者排行列表（防止缓存对象被外部修改）。
     *
     * @param source 原始列表
     * @return 深拷贝后的列表
     */
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

    /**
     * 深拷贝文章摘要 DTO。
     *
     * @param source 原始 DTO
     * @return 拷贝后的 DTO，source 为 null 时返回 null
     */
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

    /**
     * 深拷贝用户 DTO（仅拷贝排行榜展示所需字段）。
     *
     * @param source 原始用户 DTO
     * @return 拷贝后的用户 DTO
     */
    private UserDTO copyUser(UserDTO source) {
        UserDTO dto = new UserDTO();
        dto.setId(source.getId());
        dto.setUsername(source.getUsername());
        dto.setEmail(source.getEmail());
        dto.setNickname(source.getNickname());
        dto.setAvatarUrl(source.getAvatarUrl());
        dto.setBio(source.getBio());
        dto.setRole(source.getRole());
        dto.setCurrentLevel(source.getCurrentLevel());
        dto.setPrivilegeCodes(source.getPrivilegeCodes());
        dto.setExclusiveBadgeEnabled(source.isExclusiveBadgeEnabled());
        dto.setEquippedBadge(source.getEquippedBadge());
        return dto;
    }
}
