package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.command.CreateArticleCommand;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ArticlePublishValidationDTO;
import com.myblog.application.dto.ArticleVersionDTO;
import com.myblog.application.event.ArticlePublishedEvent;
import com.myblog.application.event.ArticleViewedEvent;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.application.query.RecommendArticleCacheKey;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleFavoriteRepository;
import com.myblog.domain.repository.ArticleLikeRepository;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ArticleVersionRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.entity.ArticleVersionDO;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文章应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ArticleAppService {

    private static final Logger log = LoggerFactory.getLogger(ArticleAppService.class);

    private static final String LEGACY_DEFAULT_COVER_URL = "/api/uploads/files/default/article-cover.png";
    private static final String DEFAULT_COVER_URL = "/api/uploads/files/default/article-cover.svg";
    private static final int MAX_VERSION_COUNT = 20;
    private static final int RECOMMEND_CACHE_WARM_PAGE_SIZE = 10;
    private static final DateTimeFormatter VERSION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final ArticleAssembler articleAssembler;
    private final ApplicationEventPublisher eventPublisher;
    private final ArticleVersionRepository articleVersionRepository;
    private final SensitiveWordAppService sensitiveWordAppService;
    private final HomePortalCacheInvalidator homePortalCacheInvalidator;
    private final String defaultArticleCoverUrl;
    private final Cache<RecommendArticleCacheKey, PageResult<Article>> recommendedArticleFeedCache;
    private final Set<RecommendArticleCacheKey> recommendedArticleCacheKeys = ConcurrentHashMap.newKeySet();

    public ArticleAppService(ArticleRepository articleRepository,
                             ArticleLikeRepository articleLikeRepository,
                             ArticleFavoriteRepository articleFavoriteRepository,
                             UserFollowRepository userFollowRepository,
                             UserRepository userRepository,
                             ArticleAssembler articleAssembler,
                             ApplicationEventPublisher eventPublisher,
                             ArticleVersionRepository articleVersionRepository,
                             SensitiveWordAppService sensitiveWordAppService,
                             HomePortalCacheInvalidator homePortalCacheInvalidator,
                             @Value("${my-blog.default-article-cover-url:}") String defaultArticleCoverUrl,
                             @Qualifier("recommendedArticleFeedCache")
                             Cache<RecommendArticleCacheKey, PageResult<Article>> recommendedArticleFeedCache) {
        this.articleRepository = articleRepository;
        this.articleLikeRepository = articleLikeRepository;
        this.articleFavoriteRepository = articleFavoriteRepository;
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.articleAssembler = articleAssembler;
        this.eventPublisher = eventPublisher;
        this.articleVersionRepository = articleVersionRepository;
        this.sensitiveWordAppService = sensitiveWordAppService;
        this.homePortalCacheInvalidator = homePortalCacheInvalidator;
        this.defaultArticleCoverUrl = StringUtils.hasText(defaultArticleCoverUrl)
            ? defaultArticleCoverUrl : DEFAULT_COVER_URL;
        this.recommendedArticleFeedCache = recommendedArticleFeedCache;
    }

    /**
     * 分页查询已发布文章。
     *
     * @param query 文章分页查询参数
     * @return 文章分页结果
     */
    public PageResult<ArticleDTO> pagePublishedArticles(ArticlePageQuery query) {
        int page = Math.max(query.getPage(), 1);
        int pageSize = Math.max(query.getPageSize(), 1);
        int offset = (page - 1) * pageSize;

        // Check if enhanced search is needed
        boolean needsEnhancedSearch = query.getAuthorKeyword() != null && !query.getAuthorKeyword().isEmpty()
            || query.getDateFrom() != null && !query.getDateFrom().isEmpty()
            || query.getDateTo() != null && !query.getDateTo().isEmpty()
            || (query.getKeyword() != null && !query.getKeyword().isEmpty());

        // Use full-text search if keyword length >= 2 (ngram minimum token size)
        String kw = query.getKeyword();
        boolean useFulltext = kw != null && kw.trim().length() >= 2;

        List<Article> articles;
        long total;

        if (isRecommendFeedCacheable(query, needsEnhancedSearch)) {
            PageResult<Article> recommendedPage = loadRecommendedArticlePage(
                query.getCategory(),
                page,
                pageSize
            );
            List<ArticleDTO> items = toDTOList(recommendedPage.getItems(), query.getCurrentUserId());
            return new PageResult<ArticleDTO>(items, page, pageSize, recommendedPage.getTotal());
        }

        if (query.isFollowingOnly() && query.getCurrentUserId() != null) {
            // Get following authors
            List<Long> followingIds = getFollowingAuthorIds(query.getCurrentUserId());
            if (followingIds.isEmpty()) {
                return new PageResult<>(new ArrayList<>(), page, pageSize, 0);
            }
            if (needsEnhancedSearch) {
                articles = articleRepository.findPublishedEnhancedByAuthorIds(
                    followingIds,
                    query.getKeyword(),
                    query.getCategory(),
                    query.getTag(),
                    query.getSort(),
                    query.getAuthorKeyword(),
                    query.getDateFrom(),
                    query.getDateTo(),
                    page,
                    pageSize
                );
                total = articleRepository.countPublishedEnhancedByAuthorIds(
                    followingIds,
                    query.getKeyword(),
                    query.getCategory(),
                    query.getTag(),
                    query.getSort(),
                    query.getAuthorKeyword(),
                    query.getDateFrom(),
                    query.getDateTo()
                );
            } else {
                articles = articleRepository.findPublishedByAuthorIds(
                    followingIds,
                    query.getSort(),
                    page,
                    pageSize
                );
                total = articleRepository.countPublishedByAuthorIds(followingIds, query.getSort());
            }
        } else if (needsEnhancedSearch) {
            articles = articleRepository.findPublishedEnhanced(
                query.getKeyword(),
                query.getCategory(),
                query.getTag(),
                query.getSort(),
                query.getAuthorKeyword(),
                query.getDateFrom(),
                query.getDateTo(),
                pageSize,
                offset,
                useFulltext
            );
            total = articleRepository.countPublishedEnhanced(
                query.getKeyword(),
                query.getCategory(),
                query.getTag(),
                query.getSort(),
                query.getAuthorKeyword(),
                query.getDateFrom(),
                query.getDateTo(),
                useFulltext
            );
        } else {
            articles = articleRepository.findPublishedWithLimit(
                query.getKeyword(),
                query.getCategory(),
                query.getTag(),
                query.getSort(),
                pageSize,
                offset
            );
            total = articleRepository.countPublished(
                query.getKeyword(),
                query.getCategory(),
                query.getTag(),
                query.getSort()
            );
        }

        List<ArticleDTO> items = toDTOList(articles, query.getCurrentUserId());
        return new PageResult<ArticleDTO>(items, page, pageSize, total);
    }

    /**
     * 应用启动完成后预热推荐文章 Feed 缓存。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmRecommendedArticleFeedCacheOnStartup() {
        RecommendArticleCacheKey defaultKey = RecommendArticleCacheKey.of(null, 1, RECOMMEND_CACHE_WARM_PAGE_SIZE);
        recommendedArticleCacheKeys.add(defaultKey);
        refreshRecommendedArticleCache(defaultKey);
    }

    /**
     * 定时刷新推荐文章 Feed 缓存（默认每小时一次）。
     */
    @Scheduled(cron = "${my-blog.article.recommend-refresh-cron:0 0 * * * *}")
    public void refreshRecommendedArticleFeedCache() {
        Set<RecommendArticleCacheKey> keys = new HashSet<RecommendArticleCacheKey>(recommendedArticleCacheKeys);
        if (keys.isEmpty()) {
            keys.add(RecommendArticleCacheKey.of(null, 1, RECOMMEND_CACHE_WARM_PAGE_SIZE));
        }
        for (RecommendArticleCacheKey key : keys) {
            refreshRecommendedArticleCache(key);
        }
    }

    /**
     * 判断当前查询是否可以使用推荐 Feed 缓存。
     *
     * @param query               文章分页查询参数
     * @param needsEnhancedSearch 是否需要增强搜索
     * @return true 表示可用缓存
     */
    private boolean isRecommendFeedCacheable(ArticlePageQuery query, boolean needsEnhancedSearch) {
        return ArticlePageQuery.SORT_RECOMMEND.equals(query.getSort())
            && !query.isFollowingOnly()
            && !needsEnhancedSearch
            && !StringUtils.hasText(query.getKeyword())
            && !StringUtils.hasText(query.getTag());
    }

    /**
     * 加载推荐文章分页（优先读缓存）。
     *
     * @param category 分类筛选
     * @param page     页码
     * @param pageSize 每页数量
     * @return 文章分页结果
     */
    private PageResult<Article> loadRecommendedArticlePage(String category, int page, int pageSize) {
        RecommendArticleCacheKey key = RecommendArticleCacheKey.of(category, page, pageSize);
        recommendedArticleCacheKeys.add(key);
        PageResult<Article> cached = recommendedArticleFeedCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        return refreshRecommendedArticleCache(key);
    }

    /**
     * 刷新指定 Key 对应的推荐文章缓存。
     *
     * @param key 缓存键
     * @return 刷新后的文章分页结果
     */
    private PageResult<Article> refreshRecommendedArticleCache(RecommendArticleCacheKey key) {
        PageResult<Article> page = queryRecommendedArticlePage(key);
        recommendedArticleFeedCache.put(key, page);
        return page;
    }

    /**
     * 从仓储层查询推荐文章分页。
     *
     * @param key 缓存键
     * @return 文章分页结果
     */
    private PageResult<Article> queryRecommendedArticlePage(RecommendArticleCacheKey key) {
        int currentPage = Math.max(key.getPage(), 1);
        int currentPageSize = Math.max(key.getPageSize(), 1);
        int offset = (currentPage - 1) * currentPageSize;
        String category = StringUtils.hasText(key.getCategory()) ? key.getCategory() : null;
        List<Article> articles = articleRepository.findPublishedWithLimit(
            null,
            category,
            null,
            ArticlePageQuery.SORT_RECOMMEND,
            currentPageSize,
            offset
        );
        long total = articleRepository.countPublished(
            null,
            category,
            null,
            ArticlePageQuery.SORT_RECOMMEND
        );
        return new PageResult<Article>(articles, currentPage, currentPageSize, total);
    }

    /**
     * 批量将文章领域对象转换为 DTO，同时填充点赞、收藏和关注状态。
     *
     * @param articles      文章领域对象列表
     * @param currentUserId 当前用户 ID（未登录时为 null）
     * @return 文章 DTO 列表
     */
    private List<ArticleDTO> toDTOList(List<Article> articles, Long currentUserId) {
        if (articles.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查询作者，解决 N+1 问题
        List<Long> authorIds = articles.stream()
            .map(a -> a.getAuthorId().getValue())
            .distinct()
            .collect(Collectors.toList());
        Map<Long, User> authorMap = userRepository.findByIds(authorIds).stream()
            .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u));

        // 批量查询点赞状态和收藏状态，解决 N+1 问题
        List<Long> articleIds = articles.stream()
            .map(a -> a.getId().getValue())
            .collect(Collectors.toList());
        Set<Long> likedArticleIds = Collections.emptySet();
        Set<Long> favoritedArticleIds = Collections.emptySet();
        Set<Long> followedAuthorIds = Collections.emptySet();
        if (currentUserId != null) {
            UserId currentUser = new UserId(currentUserId);
            likedArticleIds = articleLikeRepository.findLikedArticleIdsByUser(articleIds, currentUser);
            favoritedArticleIds = articleFavoriteRepository.findFavoritedArticleIdsByUser(articleIds, currentUser);
            // 批量查询已关注的作者
            followedAuthorIds = new java.util.HashSet<>(
                userFollowRepository.findFollowingUserIdsIn(currentUser, authorIds)
            );
        }

        final Set<Long> finalLikedArticleIds = likedArticleIds;
        final Set<Long> finalFavoritedArticleIds = favoritedArticleIds;
        final Set<Long> finalFollowedAuthorIds = followedAuthorIds;

        List<ArticleDTO> items = new ArrayList<>(articles.size());
        for (Article article : articles) {
            User author = authorMap.get(article.getAuthorId().getValue());
            if (author != null) {
                ArticleDTO dto = articleAssembler.toDTO(article, author);
                dto.setLiked(finalLikedArticleIds.contains(article.getId().getValue()));
                dto.setFavorited(finalFavoritedArticleIds.contains(article.getId().getValue()));
                if (dto.getAuthor() != null) {
                    boolean followed = currentUserId != null
                        && !article.getAuthorId().getValue().equals(currentUserId)
                        && finalFollowedAuthorIds.contains(article.getAuthorId().getValue());
                    dto.getAuthor().setFollowed(followed);
                }
                items.add(dto);
            }
        }
        return items;
    }

    /**
     * 获取当前用户已关注的作者 ID 列表。
     *
     * @param userId 当前用户 ID
     * @return 已关注作者 ID 列表
     */
    private List<Long> getFollowingAuthorIds(Long userId) {
        return userFollowRepository.findFollowingUserIds(new UserId(userId));
    }

    /**
     * 获取文章详情并增加阅读量。
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @Transactional(readOnly = true)
    public ArticleDTO getArticleDetail(Long articleId, Long currentUserId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!canAccessArticle(article, currentUserId, currentUserRole)) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            eventPublisher.publishEvent(new ArticleViewedEvent(articleId));
        }
        return buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), currentUserId);
    }

    /**
     * 获取相关文章（同分类，排除自身，按热度降序）。
     *
     * @param articleId 当前文章 ID
     * @param limit 返回数量
     * @return 相关文章列表
     */
    public List<ArticleDTO> getRelatedArticles(Long articleId, int limit) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        int safeLimit = (limit > 0 && limit <= 20) ? limit : 5;
        List<Article> related = articleRepository.findRelated(article.getCategory(), articleId, safeLimit);
        List<ArticleDTO> items = new ArrayList<>(related.size());
        for (Article rel : related) {
            User author = userRepository.findById(rel.getAuthorId()).orElse(null);
            if (author != null) {
                items.add(articleAssembler.toDTO(rel, author));
            }
        }
        return items;
    }

    /**
     * 创建文章。
     *
     * @param command 创建文章命令
     * @return 创建后的文章
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO createArticle(CreateArticleCommand command) {
        long _start = System.currentTimeMillis();
        User author = userRepository.findById(new UserId(command.getAuthorId()))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        ensureSlugAvailable(command.getSlug(), null);
        ArticleStatus targetStatus = resolveCreateStatus(command.getStatus());
        SanitizedArticleContent sanitizedContent = sanitizeArticleContent(
            command.getTitle(),
            command.getSummary(),
            command.getContent(),
            resolveActionText(targetStatus)
        );
        LocalDateTime scheduledPublishAt = ArticleStatus.SCHEDULED.equals(targetStatus)
            ? parseScheduledPublishAt(command.getScheduledPublishAt()) : null;
        Article article = Article.create(
            articleRepository.nextId(),
            author.getId(),
            sanitizedContent.getTitle(),
            sanitizedContent.getSummary(),
            sanitizedContent.getContent(),
            resolveCoverUrl(command.getCoverUrl()),
            command.getCategory(),
            command.getTags(),
            targetStatus,
            command.getSlug(),
            command.getSeoTitle(),
            command.getSeoDescription(),
            command.isNeedUnlock(),
            command.getUnlockPointPrice(),
            scheduledPublishAt
        );
        applyWarnFlag(article, sanitizedContent);
        articleRepository.save(article);
        if (ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                article.getId().getValue(), article.getAuthorId().getValue()));
            homePortalCacheInvalidator.evictStatsAndBootstrap();
            homePortalCacheInvalidator.evictRecommendedArticles();
        }
        saveVersionSnapshot(article, command.getAuthorId());
        ArticleDTO result = buildDetailDto(article, author, command.getAuthorId());
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(author.getId().getValue(), author.getNickname()),
            "创建文章",
            BizLogHelper.params("title", command.getTitle(), "status", targetStatus),
            BizLogHelper.created("articleId", article.getId().getValue(), "status=" + article.getStatus()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 获取编辑态文章。
     *
     * @param articleId 文章 ID
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 文章详情
     */
    public ArticleDTO getArticleForEdit(Long articleId, Long userId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        return buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), userId);
    }

    /**
     * 更新文章。
     *
     * @param articleId 文章 ID
     * @param command 更新命令
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 更新后的文章
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO updateArticle(Long articleId, CreateArticleCommand command, Long userId, String currentUserRole) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        ensureSlugAvailable(command.getSlug(), articleId);
        ArticleStatus targetStatus = resolveCreateStatus(command.getStatus());
        SanitizedArticleContent sanitizedContent = sanitizeArticleContent(
            command.getTitle(),
            command.getSummary(),
            command.getContent(),
            resolveActionText(targetStatus)
        );
        LocalDateTime scheduledPublishAt = ArticleStatus.SCHEDULED.equals(targetStatus)
            ? parseScheduledPublishAt(command.getScheduledPublishAt()) : null;
        ArticleStatus oldStatus = article.getStatus();
        article.updateContent(
            sanitizedContent.getTitle(),
            sanitizedContent.getSummary(),
            sanitizedContent.getContent(),
            resolveCoverUrl(command.getCoverUrl()),
            command.getCategory(),
            command.getTags(),
            command.getSlug(),
            command.getSeoTitle(),
            command.getSeoDescription()
        );
        article.updateUnlockRule(command.isNeedUnlock(), command.getUnlockPointPrice());
        applyStatus(article, targetStatus, scheduledPublishAt);
        applyWarnFlag(article, sanitizedContent);
        articleRepository.save(article);
        if (ArticleStatus.PUBLISHED.equals(article.getStatus()) && oldStatus != ArticleStatus.PUBLISHED) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                article.getId().getValue(), article.getAuthorId().getValue()));
        }
        evictArticlePortalCaches(article, oldStatus);
        saveVersionSnapshot(article, userId);
        ArticleDTO result = buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), userId);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "更新文章",
            BizLogHelper.params("articleId", articleId, "title", command.getTitle(), "status", targetStatus),
            BizLogHelper.statusChanged(String.valueOf(oldStatus), String.valueOf(article.getStatus())),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 更新文章状态。
     *
     * @param articleId 文章 ID
     * @param status 目标状态
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 更新后的文章
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO updateArticleStatus(Long articleId, String status, Long userId, String currentUserRole) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        ArticleStatus oldStatus = article.getStatus();
        if (ArticleStatus.PUBLISHED.name().equals(status)) {
            sanitizeExistingArticleForPublish(article, "发布文章");
        }
        applyStatus(article, status);
        articleRepository.save(article);
        if (ArticleStatus.PUBLISHED.equals(article.getStatus()) && oldStatus != ArticleStatus.PUBLISHED) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                article.getId().getValue(), article.getAuthorId().getValue()));
        }
        evictArticlePortalCaches(article, oldStatus);
        ArticleDTO result = buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), userId);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "变更文章状态",
            BizLogHelper.params("articleId", articleId, "targetStatus", status),
            BizLogHelper.statusChanged(String.valueOf(oldStatus), String.valueOf(article.getStatus())),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 删除文章。
     *
     * @param articleId 文章 ID
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long articleId, Long userId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        ArticleStatus oldStatus = article.getStatus();
        article.delete();
        articleRepository.save(article);
        evictArticlePortalCaches(article, oldStatus);
    }

    /**
     * 定时发布到期文章。
     */
    @Scheduled(fixedDelayString = "${my-blog.article.scheduled-publish-interval-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void publishDueScheduledArticles() {
        long _start = System.currentTimeMillis();
        List<Article> articles = articleRepository.findDueScheduled(LocalDateTime.now(), 20);
        if (articles.isEmpty()) {
            return;
        }
        int publishedCount = 0;
        boolean featuredCacheInvalidationNeeded = false;
        for (Article article : articles) {
            try {
                sanitizeExistingArticleForPublish(article, "定时发布文章");
                ArticleStatus oldStatus = article.getStatus();
                article.publish();
                articleRepository.save(article);
                eventPublisher.publishEvent(new ArticlePublishedEvent(
                    article.getId().getValue(), article.getAuthorId().getValue()));
                featuredCacheInvalidationNeeded = featuredCacheInvalidationNeeded
                    || isFeaturedVisibilityChanged(article, oldStatus);
                saveVersionSnapshot(article, article.getAuthorId().getValue());
                publishedCount++;
                log.info("{} | 系统 定时发布文章 | 入参({}) | 结果({}) | {}",
                    BizLogHelper.trace(),
                    BizLogHelper.params("articleId", article.getId().getValue(), "title", article.getTitle()),
                    BizLogHelper.result("published=true"),
                    BizLogHelper.elapsed(_start));
            } catch (RuntimeException ex) {
                log.warn("定时发布文章失败，articleId={}, reason={}", article.getId().getValue(), ex.getMessage());
            }
        }
        if (publishedCount > 0) {
            homePortalCacheInvalidator.evictStatsAndBootstrap();
            homePortalCacheInvalidator.evictRecommendedArticles();
        }
        if (featuredCacheInvalidationNeeded) {
            homePortalCacheInvalidator.evictFeaturedAndBootstrap();
        }
        log.info("{} | 系统 定时发布扫描 | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.result("dueCount=" + articles.size() + ", publishedCount=" + publishedCount),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 将单篇文章领域对象转换为 DTO。
     *
     * @param article       文章领域对象
     * @param currentUserId 当前用户 ID
     * @return 文章 DTO
     */
    private ArticleDTO toDTO(Article article, Long currentUserId) {
        User author = userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
        return buildDto(article, author, currentUserId);
    }

    /**
     * 构建文章分页结果（内存切片分页）。
     *
     * @param articles 全量文章列表
     * @param page     页码
     * @param pageSize 每页数量
     * @return 文章分页结果
     */
    private PageResult<ArticleDTO> buildPageResult(List<Article> articles, int page, int pageSize) {
        int fromIndex = Math.min((page - 1) * pageSize, articles.size());
        int toIndex = Math.min(fromIndex + pageSize, articles.size());
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        for (Article article : articles.subList(fromIndex, toIndex)) {
            items.add(toDTO(article, null));
        }
        return new PageResult<ArticleDTO>(items, page, pageSize, articles.size());
    }

    /**
     * 判断当前用户是否有权访问指定文章。
     *
     * @param article         文章领域对象
     * @param currentUserId   当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return true 表示可以访问
     */
    private boolean canAccessArticle(Article article, Long currentUserId, String currentUserRole) {
        if (ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            return true;
        }
        if (currentUserId == null) {
            return false;
        }
        return article.getAuthorId().getValue().equals(currentUserId)
            || UserRole.ADMIN.name().equals(currentUserRole);
    }

    /**
     * 确保当前用户有权管理指定文章（作者或管理员），无权时抛出异常。
     *
     * @param article         文章领域对象
     * @param userId          当前用户 ID
     * @param currentUserRole 当前用户角色
     */
    private void ensureCanManage(Article article, Long userId, String currentUserRole) {
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (article.getAuthorId().getValue().equals(userId) || UserRole.ADMIN.name().equals(currentUserRole)) {
            return;
        }
        throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作这篇文章");
    }

    /**
     * 解析创建/更新文章时的目标状态，默认返回草稿状态。
     *
     * @param status 状态字符串
     * @return 文章状态枚举
     */
    private ArticleStatus resolveCreateStatus(String status) {
        if (ArticleStatus.PUBLISHED.name().equals(status)) {
            return ArticleStatus.PUBLISHED;
        }
        if (ArticleStatus.SCHEDULED.name().equals(status)) {
            return ArticleStatus.SCHEDULED;
        }
        if (ArticleStatus.OFFLINE.name().equals(status)) {
            return ArticleStatus.OFFLINE;
        }
        return ArticleStatus.DRAFT;
    }

    /**
     * 将文章状态应用到领域对象。
     *
     * @param article              文章领域对象
     * @param status               目标状态
     * @param scheduledPublishAt   定时发布时间（仅定时发布时有效）
     */
    private void applyStatus(Article article, ArticleStatus status, LocalDateTime scheduledPublishAt) {
        if (status == null) {
            article.saveDraft();
            return;
        }
        if (ArticleStatus.PUBLISHED.equals(status)) {
            article.publish();
            return;
        }
        if (ArticleStatus.SCHEDULED.equals(status)) {
            article.schedulePublish(scheduledPublishAt);
            return;
        }
        if (ArticleStatus.OFFLINE.equals(status)) {
            article.offline();
            return;
        }
        if (!ArticleStatus.DRAFT.equals(status)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不支持的文章状态");
        }
        article.saveDraft();
    }

    /**
     * 将文章状态应用到领域对象（按字符串解析状态）。
     *
     * @param article 文章领域对象
     * @param status  状态字符串
     */
    private void applyStatus(Article article, String status) {
        applyStatus(article, resolveCreateStatus(status), null);
    }

    /**
     * 根据文章状态变化清除相关的门户页缓存。
     *
     * @param article   文章领域对象
     * @param oldStatus 变化前的旧状态
     */
    private void evictArticlePortalCaches(Article article, ArticleStatus oldStatus) {
        boolean featuredCacheInvalidationNeeded = isFeaturedVisibilityChanged(article, oldStatus);
        if (featuredCacheInvalidationNeeded) {
            homePortalCacheInvalidator.evictFeaturedAndBootstrap();
        }
        if (isPublishVisibilityChanged(oldStatus, article.getStatus())) {
            homePortalCacheInvalidator.evictStatsAndBootstrap();
        }
        if (!featuredCacheInvalidationNeeded && isRecommendFeedAffected(article, oldStatus)) {
            homePortalCacheInvalidator.evictRecommendedArticles();
        }
    }

    /**
     * 判断发布可见性是否发生变化（即发布/取消发布）。
     *
     * @param oldStatus 变化前状态
     * @param newStatus 变化后状态
     * @return true 表示可见性发生变化
     */
    private boolean isPublishVisibilityChanged(ArticleStatus oldStatus, ArticleStatus newStatus) {
        return ArticleStatus.PUBLISHED.equals(oldStatus) != ArticleStatus.PUBLISHED.equals(newStatus);
    }

    /**
     * 判断精选文章可见性是否发生变化。
     *
     * @param article   文章领域对象
     * @param oldStatus 变化前状态
     * @return true 表示精选可见性发生变化
     */
    private boolean isFeaturedVisibilityChanged(Article article, ArticleStatus oldStatus) {
        return article.isFeatured() && isPublishVisibilityChanged(oldStatus, article.getStatus());
    }

    /**
     * 判断该文章状态变化是否影响推荐 Feed。
     *
     * @param article   文章领域对象
     * @param oldStatus 变化前状态
     * @return true 表示需要刷新推荐 Feed
     */
    private boolean isRecommendFeedAffected(Article article, ArticleStatus oldStatus) {
        return ArticleStatus.PUBLISHED.equals(oldStatus) || ArticleStatus.PUBLISHED.equals(article.getStatus());
    }

    /**
     * 根据目标状态获取操作描述文本（用于敕感词检测错误提示）。
     *
     * @param status 目标文章状态
     * @return 操作描述文本
     */
    private String resolveActionText(ArticleStatus status) {
        if (ArticleStatus.SCHEDULED.equals(status)) {
            return "定时发布文章";
        }
        if (ArticleStatus.PUBLISHED.equals(status)) {
            return "发布文章";
        }
        return "保存文章";
    }

    /**
     * 解析定时发布时间字符串为 LocalDateTime。
     *
     * @param value 时间字符串（ISO 8601 格式）
     * @return 定时发布时间
     */
    private LocalDateTime parseScheduledPublishAt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
        } catch (RuntimeException ex) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "定时发布时间格式不正确");
        }
    }

    /**
     * 解析封面图 URL，若为空或为旧默认封面则返回新默认封面。
     *
     * @param coverUrl 封面图 URL
     * @return 解析后的封面图 URL
     */
    private String resolveCoverUrl(String coverUrl) {
        if (StringUtils.hasText(coverUrl) && !LEGACY_DEFAULT_COVER_URL.equals(coverUrl)) {
            return coverUrl.trim();
        }
        return defaultArticleCoverUrl;
    }

    /**
     * 确保 URL Slug 可用（未被其他文章占用）。
     *
     * @param slug             要检查的 Slug
     * @param currentArticleId 当前文章 ID（更新时传入，排除自身）
     */
    private void ensureSlugAvailable(String slug, Long currentArticleId) {
        if (!StringUtils.hasText(slug)) {
            return;
        }
        String normalizedSlug = slug.trim();
        articleRepository.findBySlug(normalizedSlug).ifPresent(existing -> {
            if (currentArticleId == null || !existing.getId().getValue().equals(currentArticleId)) {
                throw new ApplicationException(ErrorCode.CONFLICT, "URL Slug 已存在，请换一个");
            }
        });
    }

    /**
     * 对文章内容进行敘感词过滤，返回处理后的内容。
     *
     * @param title   文章标题
     * @param summary 文章摘要
     * @param content 文章正文
     * @param action  操作描述（用于错误提示）
     * @return 过滤后的文章内容
     */
    private SanitizedArticleContent sanitizeArticleContent(String title, String summary,
                                                           String content, String action) {
        String sensitiveText = buildArticleSensitiveText(title, summary, content);
        List<String> blockHits = sensitiveWordAppService.detectBlockWords(sensitiveText);
        if (!blockHits.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR,
                action + "失败：内容包含被禁止的敏感词（" + String.join("、", blockHits) + "），请修改后重试");
        }
        List<String> warnHits = sensitiveWordAppService.detectWarnWords(sensitiveText);
        return new SanitizedArticleContent(
            sensitiveWordAppService.maskWarnWords(title),
            sensitiveWordAppService.maskWarnWords(summary),
            sensitiveWordAppService.maskWarnWords(content),
            !warnHits.isEmpty()
        );
    }

    /**
     * 对已存在文章进行内容效验和敘感词过滤（发布前调用）。
     *
     * @param article 文章领域对象
     * @param action  操作描述（用于错误提示）
     */
    private void sanitizeExistingArticleForPublish(Article article, String action) {
        SanitizedArticleContent sanitizedContent = sanitizeArticleContent(
            article.getTitle(),
            article.getSummary(),
            article.getContent(),
            action
        );
        article.updateContent(
            sanitizedContent.getTitle(),
            sanitizedContent.getSummary(),
            sanitizedContent.getContent(),
            article.getCoverUrl(),
            article.getCategory(),
            article.getTags(),
            article.getSlug(),
            article.getSeoTitle(),
            article.getSeoDescription()
        );
        applyWarnFlag(article, sanitizedContent);
    }

    /**
     * 根据敨感词检测结果更新文章警告标记。
     *
     * @param article          文章领域对象
     * @param sanitizedContent 敨感词过滤结果
     */
    private void applyWarnFlag(Article article, SanitizedArticleContent sanitizedContent) {
        article.updateWarnFlag(sanitizedContent.hasWarnHits());
    }

    /**
     * 构建用于敨感词检测的文章内容文本。
     *
     * @param title   标题
     * @param summary 摘要
     * @param content 正文
     * @return 合并后的文本内容
     */
    private String buildArticleSensitiveText(String title, String summary, String content) {
        return normalizeValue(title) + "\n" + normalizeValue(summary) + "\n" + normalizeValue(content);
    }

    private static class SanitizedArticleContent {
        private final String title;
        private final String summary;
        private final String content;
        private final boolean warnHits;

        SanitizedArticleContent(String title, String summary, String content, boolean warnHits) {
            this.title = title;
            this.summary = summary;
            this.content = content;
            this.warnHits = warnHits;
        }

        String getTitle() {
            return title;
        }

        String getSummary() {
            return summary;
        }

        String getContent() {
            return content;
        }

        boolean hasWarnHits() {
            return warnHits;
        }
    }

    /**
     * 构建文章列表 DTO（不含正文）。
     *
     * @param article       文章领域对象
     * @param author        作者领域对象
     * @param currentUserId 当前用户 ID
     * @return 文章 DTO
     */
    private ArticleDTO buildDto(Article article, User author, Long currentUserId) {
        ArticleDTO dto = articleAssembler.toDTO(article, author);
        populateUserStatus(dto, article, currentUserId);
        return dto;
    }

    /**
     * 构建文章详情 DTO（含正文和作者粉丝数）。
     *
     * @param article       文章领域对象
     * @param author        作者领域对象
     * @param currentUserId 当前用户 ID
     * @return 文章详情 DTO
     */
    private ArticleDTO buildDetailDto(Article article, User author, Long currentUserId) {
        ArticleDTO dto = articleAssembler.toDetailDTO(article, author);
        dto.getAuthor().setFollowerCount(userRepository.countFollowers(author.getId().getValue()));
        populateUserStatus(dto, article, currentUserId);
        return dto;
    }

    /**
     * 填充文章 DTO 中当前用户的互动状态（点赞、收藏、关注）。
     *
     * @param dto           文章 DTO
     * @param article       文章领域对象
     * @param currentUserId 当前用户 ID（未登录时为 null）
     */
    private void populateUserStatus(ArticleDTO dto, Article article, Long currentUserId) {
        if (dto.getAuthor() != null) {
            dto.getAuthor().setFollowed(resolveAuthorFollowed(article, currentUserId));
        }
        if (currentUserId == null) {
            dto.setLiked(false);
            dto.setFavorited(false);
            return;
        }
        UserId currentUser = new UserId(currentUserId);
        dto.setLiked(articleLikeRepository.exists(article.getId(), currentUser));
        dto.setFavorited(articleFavoriteRepository.exists(article.getId(), currentUser));
    }

    // ── 版本历史 ──────────────────────────────────────────────────────────

    /**
     * 获取文章版本列表（仅返回元信息，不含正文）。
     *
     * @param articleId 文章 ID
     * @param userId    当前登录用户 ID
     * @param role      当前用户角色
     * @return 版本列表
     */
    public List<ArticleVersionDTO> listVersions(Long articleId, Long userId, String role) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, role);
        return articleVersionRepository.findByArticleId(articleId).stream()
            .map(this::toVersionDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取指定版本内容。
     *
     * @param articleId 文章 ID
     * @param versionNo 版本号
     * @param userId    当前登录用户 ID
     * @param role      当前用户角色
     * @return 版本详情（含正文）
     */
    public ArticleVersionDTO getVersion(Long articleId, Integer versionNo, Long userId, String role) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, role);
        return articleVersionRepository.findByArticleIdAndVersionNo(articleId, versionNo)
            .map(v -> {
                ArticleVersionDTO dto = toVersionDTO(v);
                dto.setContent(v.getContent());
                return dto;
            })
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "版本不存在"));
    }

    /**
     * 将指定版本内容恢复为新草稿（不直接覆盖已发布版本）。
     *
     * @param articleId 文章 ID
     * @param versionNo 版本号
     * @param userId    当前登录用户 ID
     * @param role      当前用户角色
     * @return 恢复后的草稿 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO restoreVersion(Long articleId, Integer versionNo, Long userId, String role) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, role);
        ArticleVersionDO version = articleVersionRepository
            .findByArticleIdAndVersionNo(articleId, versionNo)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "版本不存在"));
        SanitizedArticleContent sanitizedContent = sanitizeArticleContent(
            version.getTitle(),
            version.getSummary(),
            version.getContent(),
            "恢复文章版本"
        );
        ArticleStatus oldStatus = article.getStatus();
        // 用版本内容更新文章并保存为草稿
        article.updateContent(
            sanitizedContent.getTitle(),
            sanitizedContent.getSummary(),
            sanitizedContent.getContent(),
            article.getCoverUrl(),
            article.getCategory(),
            article.getTags(),
            article.getSlug(),
            article.getSeoTitle(),
            article.getSeoDescription()
        );
        article.saveDraft();
        applyWarnFlag(article, sanitizedContent);
        articleRepository.save(article);
        evictArticlePortalCaches(article, oldStatus);
        // 恢复操作本身也生成一条新版本快照
        saveVersionSnapshot(article, userId);
        return buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), userId);
    }

    /**
     * 保存版本快照，并自动删除超出上限的旧版本。
     */
    private void saveVersionSnapshot(Article article, Long savedBy) {
        int nextVersionNo = articleVersionRepository.findMaxVersionNo(article.getId().getValue()) + 1;
        ArticleVersionDO version = new ArticleVersionDO();
        version.setArticleId(article.getId().getValue());
        version.setVersionNo(nextVersionNo);
        version.setTitle(article.getTitle());
        version.setSummary(article.getSummary());
        version.setContent(article.getContent());
        version.setSavedBy(savedBy);
        articleVersionRepository.save(version);
        // 超出上限时删除最旧版本
        int total = articleVersionRepository.countByArticleId(article.getId().getValue());
        if (total > MAX_VERSION_COUNT) {
            articleVersionRepository.deleteOldestVersions(article.getId().getValue(), MAX_VERSION_COUNT);
        }
    }

    /**
     * 将文章版本 DO 转换为 DTO（不含正文）。
     *
     * @param v 文章版本 DO
     * @return 文章版本 DTO
     */
    private ArticleVersionDTO toVersionDTO(ArticleVersionDO v) {
        ArticleVersionDTO dto = new ArticleVersionDTO();
        dto.setVersionNo(v.getVersionNo());
        dto.setTitle(v.getTitle());
        dto.setSummary(v.getSummary());
        dto.setSavedAt(v.getCreatedAt() != null ? v.getCreatedAt().format(VERSION_TIME_FORMATTER) : null);
        return dto;
    }

    /**
     * 判断当前用户是否已关注文章作者。
     *
     * @param article       文章领域对象
     * @param currentUserId 当前用户 ID（未登录时为 null）
     * @return true 表示已关注
     */
    private boolean resolveAuthorFollowed(Article article, Long currentUserId) {
        if (currentUserId == null || article.getAuthorId().getValue().equals(currentUserId)) {
            return false;
        }
        return userFollowRepository.exists(new UserId(currentUserId), article.getAuthorId());
    }

    /**
     * 校验文章是否满足发布条件。
     *
     * @param command 当前编辑内容
     * @return 发布校验结果
     */
    public ArticlePublishValidationDTO validateDraftForPublish(CreateArticleCommand command) {
        ArticlePublishValidationDTO validation = new ArticlePublishValidationDTO();
        List<String> errors = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();
        List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();

        String title = normalizeValue(command.getTitle());
        String summary = normalizeValue(command.getSummary());
        String content = normalizeValue(command.getContent());
        String category = normalizeValue(command.getCategory());
        List<String> tags = command.getTags() == null ? Collections.<String>emptyList() : command.getTags();
        String coverUrl = resolveCoverUrl(command.getCoverUrl());
        boolean needUnlock = command.isNeedUnlock();
        int unlockPointPrice = command.getUnlockPointPrice();

        appendCheck(
            checks,
            errors,
            "title",
            "文章标题",
            StringUtils.hasText(title),
            "error",
            "标题已填写",
            "发布前请先填写文章标题"
        );
        appendCheck(
            checks,
            errors,
            "content",
            "文章正文",
            StringUtils.hasText(content),
            "error",
            "正文内容已准备好",
            "发布前请先补全文章正文"
        );
        appendCheck(
            checks,
            errors,
            "category",
            "文章分类",
            StringUtils.hasText(category),
            "error",
            "文章分类已选择",
            "发布前请先选择文章分类"
        );
        appendCheck(
            checks,
            warnings,
            "summary",
            "文章摘要",
            StringUtils.hasText(summary),
            "warning",
            "摘要已填写",
            "建议补充一段摘要，方便首页、搜索和专栏页展示"
        );
        appendCheck(
            checks,
            warnings,
            "tags",
            "文章标签",
            !tags.isEmpty(),
            "warning",
            "标签已设置",
            "建议至少补充一个标签，方便读者发现内容"
        );
        appendCheck(
            checks,
            warnings,
            "cover",
            "文章封面",
            StringUtils.hasText(coverUrl) && !defaultArticleCoverUrl.equals(coverUrl),
            "warning",
            "已设置自定义封面",
            "当前会使用系统默认封面，建议上传更贴合主题的封面图"
        );
        appendCheck(
            checks,
            errors,
            "unlockPointPrice",
            "积分解锁",
            !needUnlock || unlockPointPrice > 0,
            "error",
            needUnlock ? "已设置解锁积分" : "文章免费阅读",
            "开启积分解锁后请设置大于 0 的解锁积分"
        );

        // 敏感词检测
        String sensitiveText = buildArticleSensitiveText(title, summary, content);
        List<String> blockHits = sensitiveWordAppService.detectBlockWords(sensitiveText);
        if (!blockHits.isEmpty()) {
            String msg = "内容包含被禁止的敏感词：" + String.join("、", blockHits);
            appendCheck(checks, errors, "sensitive-block", "敏感词检测", false, "error",
                "内容无敏感词", msg);
        } else {
            List<String> warnHits = sensitiveWordAppService.detectWarnWords(sensitiveText);
            if (!warnHits.isEmpty()) {
                String msg = "内容包含警告类敏感词，发布后会自动替换为 ***：" + String.join("、", warnHits);
                appendCheck(checks, warnings, "sensitive-warn", "敏感词检测", false, "warning",
                    "内容无敏感词", msg);
            }
        }

        validation.setPublishable(errors.isEmpty());
        validation.setErrors(errors);
        validation.setWarnings(warnings);
        validation.setChecks(checks);
        return validation;
    }

    /**
     * 添加一条发布当前的校验条目。
     *
     * @param checks        所有校验条目列表
     * @param messages      要收集的错误或警告消息列表
     * @param key           条目标识符
     * @param label         条目显示名称
     * @param passed        是否通过
     * @param level         级别（error/warning）
     * @param passedMessage 通过时的消息
     * @param failedMessage 未通过时的消息
     */
    private void appendCheck(List<Map<String, Object>> checks,
                             List<String> messages,
                             String key,
                             String label,
                             boolean passed,
                             String level,
                             String passedMessage,
                             String failedMessage) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("key", key);
        item.put("label", label);
        item.put("passed", passed);
        item.put("level", level);
        item.put("message", passed ? passedMessage : failedMessage);
        checks.add(item);
        if (!passed) {
            messages.add(failedMessage);
        }
    }

    /**
     * 规范化字符串将6，null 转化为空字符串并去除首尾空格。
     *
     * @param source 原始字符串
     * @return 规范化后的字符串
     */
    private String normalizeValue(String source) {
        return source == null ? "" : source.trim();
    }

    /**
     * 导出指定用户的文章为 ZIP，每篇文章生成一个 Markdown 文件。
     *
     * @param userId 用户 ID
     * @return ZIP 字节数组
     */
    public byte[] exportMyArticlesZip(Long userId) {
        List<Article> articles = articleRepository.findByAuthorId(userId);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            for (Article article : articles) {
                String safeName = article.getTitle() == null ? "untitled" : article.getTitle()
                    .replaceAll("[/\\\\:*?\"<>|]", "_").trim();
                if (safeName.isEmpty()) safeName = "article-" + article.getId().getValue();
                String entryName = article.getId().getValue() + "-" + safeName + ".md";
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
                zos.putNextEntry(entry);
                String md = buildMarkdown(article);
                zos.write(md.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("生成 ZIP 失败", e);
        }
        return baos.toByteArray();
    }

    /**
     * 将文章领域对象构建为 Markdown 格式内容。
     *
     * @param article 文章领域对象
     * @return Markdown 格式的内容字符串
     */
    private String buildMarkdown(Article article) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(article.getTitle() == null ? "" : article.getTitle()).append("\n\n");
        if (article.getCategory() != null) {
            sb.append("> 分类: ").append(article.getCategory()).append("\n\n");
        }
        if (article.getSummary() != null && !article.getSummary().isEmpty()) {
            sb.append("> ").append(article.getSummary()).append("\n\n");
        }
        sb.append("---\n\n");
        sb.append(article.getContent() == null ? "" : article.getContent());
        return sb.toString();
    }

    /**
     * 获取指定文章的上下篇（均为已发布文章）。
     *
     * @param articleId 当前文章 ID
     * @return Map with keys "prev" and "next", values may be null
     */
    public java.util.Map<String, ArticleDTO> getArticleNeighbors(Long articleId) {
        java.util.Optional<com.myblog.domain.model.aggregate.Article> prevOpt =
            articleRepository.findPrevPublished(articleId);
        java.util.Optional<com.myblog.domain.model.aggregate.Article> nextOpt =
            articleRepository.findNextPublished(articleId);
        java.util.Map<String, ArticleDTO> result = new java.util.LinkedHashMap<>();
        result.put("prev", prevOpt.map(this::toNeighborDTO).orElse(null));
        result.put("next", nextOpt.map(this::toNeighborDTO).orElse(null));
        return result;
    }

    /**
     * 将文章领域对象转换为上下篇导航用 DTO。
     *
     * @param article 文章领域对象
     * @return 文章 DTO，作者不存在时返回 null
     */
    private ArticleDTO toNeighborDTO(Article article) {
        return userRepository.findById(article.getAuthorId())
            .map(author -> articleAssembler.toDTO(article, author))
            .orElse(null);
    }
}
