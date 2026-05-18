package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ArticleRecommendationSectionDTO;
import com.myblog.application.dto.ArticleRecommendationsDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.ColumnSubscriptionRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 推荐应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class RecommendationAppService {

    private static final int DEFAULT_ARTICLE_RECOMMENDATION_LIMIT = 12;
    private static final int MAX_ARTICLE_RECOMMENDATION_LIMIT = 24;
    private static final int WEEKLY_RECOMMEND_FEED_EXCLUDE_LIMIT = 10;

    private final UserRepository userRepository;
    private final ColumnRepository columnRepository;
    private final ArticleRepository articleRepository;
    private final ColumnSubscriptionRepository columnSubscriptionRepository;
    private final UserFollowRepository userFollowRepository;
    private final ArticleAssembler articleAssembler;
    private final UserLevelAppService userLevelAppService;
    private final Cache<String, List<ArticleDTO>> featuredArticlesCache;
    private final Cache<String, ArticleRecommendationsDTO> articleRecommendationsCache;

    public RecommendationAppService(UserRepository userRepository,
                                    ColumnRepository columnRepository,
                                    ArticleRepository articleRepository,
                                    ColumnSubscriptionRepository columnSubscriptionRepository,
                                    UserFollowRepository userFollowRepository,
                                    ArticleAssembler articleAssembler,
                                    UserLevelAppService userLevelAppService,
                                    @Qualifier("featuredArticlesCache")
                                    Cache<String, List<ArticleDTO>> featuredArticlesCache,
                                    @Qualifier("articleRecommendationsCache")
                                    Cache<String, ArticleRecommendationsDTO> articleRecommendationsCache) {
        this.userRepository = userRepository;
        this.columnRepository = columnRepository;
        this.articleRepository = articleRepository;
        this.columnSubscriptionRepository = columnSubscriptionRepository;
        this.userFollowRepository = userFollowRepository;
        this.articleAssembler = articleAssembler;
        this.userLevelAppService = userLevelAppService;
        this.featuredArticlesCache = featuredArticlesCache;
        this.articleRecommendationsCache = articleRecommendationsCache;
    }

    /**
     * 推荐作者列表。
     */
    public List<UserDTO> listRecommendedAuthors(int limit, Long currentUserId) {
        List<User> users = userRepository.findRecommended(limit);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> authorIds = new ArrayList<>(users.size());
        for (User user : users) {
            authorIds.add(user.getId().getValue());
        }

        java.util.Map<Long, com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO> statsMap =
            new java.util.HashMap<>();
        List<com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO> statsList =
            articleRepository.findAuthorArticleStatsByAuthorIds(authorIds);
        for (com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO stats : statsList) {
            statsMap.put(stats.getAuthorId(), stats);
        }

        List<UserDTO> items = new ArrayList<>(users.size());
        for (User user : users) {
            UserDTO dto = UserAssembler.toDTO(user);
            if (currentUserId != null) {
                dto.setFollowed(userFollowRepository.exists(new UserId(currentUserId), user.getId()));
            }
            com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO stats = statsMap.get(user.getId().getValue());
            if (stats != null) {
                dto.setArticleCount(stats.getArticleCount() != null ? stats.getArticleCount() : 0);
                dto.setTotalLikeCount(stats.getTotalLikes() != null ? stats.getTotalLikes() : 0L);
            }
            items.add(dto);
        }
        userLevelAppService.fillLevels(items);
        return items;
    }

    /**
     * 推荐专栏列表。
     */
    public List<ColumnDTO> listRecommendedColumns(int limit, Long currentUserId) {
        List<Column> columns = columnRepository.findRecommended(limit);
        List<ColumnDTO> items = new ArrayList<>(columns.size());
        for (Column column : columns) {
            ColumnDTO dto = toColumnDTO(column, currentUserId);
            items.add(dto);
        }
        return items;
    }

    /**
     * 推荐/精选文章列表。
     */
    public List<ArticleDTO> listFeaturedArticles(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        String cacheKey = currentPage + ":" + currentPageSize;
        List<ArticleDTO> cached = featuredArticlesCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Article> articles = articleRepository.findFeatured(currentPage, currentPageSize);
        List<ArticleDTO> items = new ArrayList<>(articles.size());
        for (Article article : articles) {
            User author = userRepository.findById(article.getAuthorId()).orElse(null);
            if (author == null) {
                continue;
            }
            items.add(articleAssembler.toDTO(article, author));
        }
        userLevelAppService.fillLevels(items.stream()
            .map(ArticleDTO::getAuthor)
            .collect(java.util.stream.Collectors.toList()));
        featuredArticlesCache.put(cacheKey, items);
        return items;
    }

    /**
     * 本周必读算法文章列表，不使用管理员精选权重排序。
     */
    public List<ArticleDTO> listWeeklyArticles(Long excludeArticleId, int limit) {
        int safeLimit = Math.max(limit, 1);
        List<Long> excludedArticleIds = buildWeeklyExcludedArticleIds(excludeArticleId);
        List<Article> articles = articleRepository.findWeeklyPicks(excludedArticleIds, safeLimit);
        List<ArticleDTO> items = new ArrayList<>(articles.size());
        for (Article article : articles) {
            User author = userRepository.findById(article.getAuthorId()).orElse(null);
            if (author == null) {
                continue;
            }
            items.add(articleAssembler.toDTO(article, author));
        }
        userLevelAppService.fillLevels(items.stream()
            .map(ArticleDTO::getAuthor)
            .collect(java.util.stream.Collectors.toList()));
        return items;
    }

    private List<Long> buildWeeklyExcludedArticleIds(Long focusArticleId) {
        Set<Long> excludedArticleIds = new HashSet<Long>();
        if (focusArticleId != null) {
            excludedArticleIds.add(focusArticleId);
        }
        List<Article> recommendedHeadArticles = articleRepository.findPublishedWithLimit(
            null,
            null,
            null,
            ArticlePageQuery.SORT_RECOMMEND,
            WEEKLY_RECOMMEND_FEED_EXCLUDE_LIMIT,
            0
        );
        for (Article article : recommendedHeadArticles) {
            if (article != null && article.getId() != null) {
                excludedArticleIds.add(article.getId().getValue());
            }
        }
        return new ArrayList<Long>(excludedArticleIds);
    }

    /**
     * 文章详情页推荐分组。
     */
    public ArticleRecommendationsDTO getArticleRecommendations(Long articleId, int limit) {
        int safeLimit = normalizeRecommendationLimit(limit);
        String cacheKey = articleId + ":" + safeLimit;
        ArticleRecommendationsDTO cached = articleRecommendationsCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        int sectionLimit = Math.max(3, Math.min(6, (int) Math.ceil(safeLimit / 3.0)));
        Set<Long> seenIds = new HashSet<Long>();
        seenIds.add(articleId);

        List<ArticleRecommendationSectionDTO> sections = new ArrayList<ArticleRecommendationSectionDTO>();
        appendSection(
            sections,
            "same_column",
            "继续读这个专栏",
            toUniqueArticleDTOs(
                articleRepository.findPublishedInSameColumns(articleId, articleId, sectionLimit * 2),
                seenIds,
                sectionLimit
            )
        );
        appendSection(
            sections,
            "same_author",
            "这位作者的更多文章",
            toUniqueArticleDTOs(
                articleRepository.findPublishedByAuthorIdExcluding(
                    article.getAuthorId().getValue(),
                    articleId,
                    sectionLimit * 2
                ),
                seenIds,
                sectionLimit
            )
        );

        int remainingLimit = Math.max(0, safeLimit - countRecommendedItems(sections));
        List<ArticleDTO> relatedItems = toUniqueArticleDTOs(
            articleRepository.findPublishedBySignals(
                article.getTags(),
                article.getCategory(),
                articleId,
                safeLimit * 2
            ),
            seenIds,
            remainingLimit
        );
        int fallbackLimit = safeLimit - countRecommendedItems(sections) - relatedItems.size();
        if (fallbackLimit > 0) {
            relatedItems.addAll(toUniqueArticleDTOs(
                articleRepository.findPopularPublishedExcluding(new ArrayList<Long>(seenIds), safeLimit),
                seenIds,
                fallbackLimit
            ));
        }
        appendSection(sections, "related", "相关推荐", relatedItems);

        ArticleRecommendationsDTO recommendations = new ArticleRecommendationsDTO(sections);
        articleRecommendationsCache.put(cacheKey, recommendations);
        return recommendations;
    }

    public void evictFeaturedArticles() {
        featuredArticlesCache.invalidateAll();
    }

    private int normalizeRecommendationLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_ARTICLE_RECOMMENDATION_LIMIT;
        }
        return Math.min(limit, MAX_ARTICLE_RECOMMENDATION_LIMIT);
    }

    private void appendSection(List<ArticleRecommendationSectionDTO> sections,
                               String key,
                               String title,
                               List<ArticleDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sections.add(new ArticleRecommendationSectionDTO(key, title, items));
    }

    private int countRecommendedItems(List<ArticleRecommendationSectionDTO> sections) {
        int total = 0;
        for (ArticleRecommendationSectionDTO section : sections) {
            total += section.getItems().size();
        }
        return total;
    }

    private List<ArticleDTO> toUniqueArticleDTOs(List<Article> source, Set<Long> seenIds, int limit) {
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        if (source == null || limit <= 0) {
            return items;
        }
        for (Article article : source) {
            if (article == null || article.getId() == null) {
                continue;
            }
            Long id = article.getId().getValue();
            if (seenIds.contains(id)) {
                continue;
            }
            User author = userRepository.findById(article.getAuthorId()).orElse(null);
            if (author == null) {
                continue;
            }
            seenIds.add(id);
            items.add(articleAssembler.toDTO(article, author));
            if (items.size() >= limit) {
                break;
            }
        }
        userLevelAppService.fillLevels(items.stream()
            .map(ArticleDTO::getAuthor)
            .collect(java.util.stream.Collectors.toList()));
        return items;
    }

    private ColumnDTO toColumnDTO(Column column, Long currentUserId) {
        User author = userRepository.findById(column.getAuthorId())
            .orElse(null);
        ColumnDTO dto = new ColumnDTO();
        dto.setId(column.getId().getValue());
        dto.setTitle(column.getTitle());
        dto.setSummary(column.getSummary());
        dto.setCoverUrl(column.getCoverUrl());
        dto.setSubscriberCount(column.getSubscriberCount());
        dto.setArticleCount(column.getArticleCount());
        dto.setSubscribed(currentUserId != null
            && columnSubscriptionRepository.exists(column.getId(), new UserId(currentUserId)));
        if (author != null) {
            dto.setAuthor(UserAssembler.toDTO(author));
            userLevelAppService.fillLevel(dto.getAuthor());
        }
        return dto;
    }
}
