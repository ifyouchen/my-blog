package com.myblog.application.service;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.entity.AdminCategoryStatDO;
import com.myblog.infrastructure.repository.persistence.entity.AdminTrendPointDO;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO;
import com.myblog.application.event.ArticlePublishedEvent;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminAppService {

    private static final Logger log = LoggerFactory.getLogger(AdminAppService.class);

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final CommentAppService commentAppService;
    private final HomePortalCacheInvalidator homePortalCacheInvalidator;
    private final SensitiveWordAppService sensitiveWordAppService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminAppService(UserRepository userRepository,
                           ArticleRepository articleRepository,
                           CommentRepository commentRepository,
                           CommentAppService commentAppService,
                           HomePortalCacheInvalidator homePortalCacheInvalidator,
                           SensitiveWordAppService sensitiveWordAppService,
                           ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.commentAppService = commentAppService;
        this.homePortalCacheInvalidator = homePortalCacheInvalidator;
        this.sensitiveWordAppService = sensitiveWordAppService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 查询后台概览统计数据（含7日趋势、分类分布、Top作者）。
     *
     * @return 概览统计
     */
    public Map<String, Object> getStats() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        long userCount = userRepository.countAll();
        long normalUserCount = userRepository.countByStatus(UserStatus.NORMAL);
        long disabledUserCount = userRepository.countByStatus(UserStatus.DISABLED);
        long articleCount = articleRepository.countVisible();
        long draftCount = articleRepository.countByStatus(ArticleStatus.DRAFT.name());
        long scheduledCount = articleRepository.countByStatus(ArticleStatus.SCHEDULED.name());
        long publishedCount = articleRepository.countByStatus(ArticleStatus.PUBLISHED.name());
        long featuredCount = articleRepository.countAdminPage("FEATURED", null, null);
        long offlineCount = articleRepository.countByStatus(ArticleStatus.OFFLINE.name());
        long deletedCount = articleRepository.countByStatus(ArticleStatus.DELETED.name());
        long commentCount = commentRepository.countAll();
        long todayNewUsers = userRepository.countCreatedOn(today);
        long todayNewArticles = articleRepository.countCreatedOn(today);
        long todayNewComments = commentRepository.countCreatedOn(today);
        long sevenDayUsers = userRepository.countCreatedSince(sevenDaysAgo);
        long sevenDayArticles = articleRepository.countCreatedSince(sevenDaysAgo);
        long sevenDayComments = commentRepository.countCreatedSince(sevenDaysAgo);

        // 7 日每日趋势：合并用户、文章、评论的每日新增量
        List<Map<String, Object>> sevenDayTrend = buildSevenDayTrend(today, sevenDaysAgo);

        // 分类分布（已发布文章，Top 10）
        List<AdminCategoryStatDO> categoryStatDOs = articleRepository.findCategoryStats(10);
        List<Map<String, Object>> categoryStats = new ArrayList<Map<String, Object>>(categoryStatDOs.size());
        for (AdminCategoryStatDO stat : categoryStatDOs) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("category", stat.getCategory());
            entry.put("articleCount", stat.getArticleCount());
            categoryStats.add(entry);
        }

        // Top 5 活跃作者（按已发布文章的浏览量排序）
        List<AuthorArticleStatsDO> authorStatsDOs = articleRepository.findAuthorArticleStats(5);
        List<Long> topAuthorIds = new ArrayList<Long>(authorStatsDOs.size());
        for (AuthorArticleStatsDO stat : authorStatsDOs) {
            topAuthorIds.add(stat.getAuthorId());
        }
        List<User> topAuthors = topAuthorIds.isEmpty()
            ? Collections.emptyList()
            : userRepository.findByIds(topAuthorIds);
        Map<Long, User> authorMap = new HashMap<Long, User>(topAuthors.size());
        for (User u : topAuthors) {
            authorMap.put(u.getId().getValue(), u);
        }
        List<Map<String, Object>> topAuthorList = new ArrayList<Map<String, Object>>(authorStatsDOs.size());
        for (AuthorArticleStatsDO stat : authorStatsDOs) {
            User author = authorMap.get(stat.getAuthorId());
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("authorId", stat.getAuthorId());
            entry.put("username", author == null ? null : author.getUsername());
            entry.put("nickname", author == null ? null : author.getNickname());
            entry.put("avatarUrl", author == null ? null : author.getAvatarUrl());
            entry.put("articleCount", stat.getArticleCount());
            entry.put("totalViews", stat.getTotalViews());
            entry.put("totalLikes", stat.getTotalLikes());
            topAuthorList.add(entry);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userCount);
        stats.put("normalUsers", normalUserCount);
        stats.put("disabledUsers", disabledUserCount);
        stats.put("totalArticles", articleCount);
        stats.put("draftArticles", draftCount);
        stats.put("scheduledArticles", scheduledCount);
        stats.put("publishedArticles", publishedCount);
        stats.put("featuredArticles", featuredCount);
        stats.put("offlineArticles", offlineCount);
        stats.put("deletedArticles", deletedCount);
        stats.put("totalComments", commentCount);
        stats.put("todayNewUsers", todayNewUsers);
        stats.put("todayNewArticles", todayNewArticles);
        stats.put("todayNewComments", todayNewComments);
        stats.put("sevenDayUsers", sevenDayUsers);
        stats.put("sevenDayArticles", sevenDayArticles);
        stats.put("sevenDayComments", sevenDayComments);
        stats.put("sevenDayTrend", sevenDayTrend);
        stats.put("categoryStats", categoryStats);
        stats.put("topAuthors", topAuthorList);
        return stats;
    }

    /**
     * 构建最近 7 日每日新增用户、文章、评论趋势。
     */
    private List<Map<String, Object>> buildSevenDayTrend(LocalDate today, LocalDate sevenDaysAgo) {
        // 获取文章每日趋势（使用 DB 聚合）
        List<AdminTrendPointDO> articleTrend = articleRepository.findDailyArticleTrend(sevenDaysAgo, today);
        Map<String, Long> articleByDate = new HashMap<String, Long>();
        for (AdminTrendPointDO p : articleTrend) {
            articleByDate.put(p.getDate(), p.getNewArticles());
        }
        // 用户和评论按日循环查询
        List<Map<String, Object>> trend = new ArrayList<Map<String, Object>>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();
            long newUsers = userRepository.countCreatedOn(date);
            long newArticles = articleByDate.containsKey(dateStr) ? articleByDate.get(dateStr) : 0L;
            long newComments = commentRepository.countCreatedOn(date);
            Map<String, Object> point = new LinkedHashMap<String, Object>();
            point.put("date", dateStr);
            point.put("newUsers", newUsers);
            point.put("newArticles", newArticles);
            point.put("newComments", newComments);
            trend.add(point);
        }
        return trend;
    }

    /**
     * 分页查询后台用户列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 用户分页结果
     */
    public PageResult<Map<String, Object>> getUsers(int page, int pageSize, String status, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<User> users = userRepository.findAdminPage(status, normalizedKeyword, page, pageSize);
        long total = userRepository.countAdminPage(status, normalizedKeyword);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(users.size());
        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId().getValue());
            map.put("username", user.getUsername());
            map.put("email", user.getEmail().getValue());
            map.put("nickname", user.getNickname());
            map.put("status", user.getStatus().name());
            map.put("role", user.getRole().name());
            map.put("disableReason", user.getDisableReason());
            map.put("createdAt", user.getCreatedAt());
            map.put("updatedAt", user.getUpdatedAt());
            map.put("recommended", user.isRecommended());
            items.add(map);
        }
        return new PageResult<Map<String, Object>>(items, page, pageSize, total);
    }

    /**
     * 更新用户状态。
     *
     * @param userId 用户 ID
     * @param status 目标状态
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateUserStatus(Long userId, String status) {
        long _start = System.currentTimeMillis();
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserStatus newStatus = UserStatus.valueOf(status);
        UserStatus previousStatus = user.getStatus();
        user.updateStatus(newStatus);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId().getValue());
        result.put("username", user.getUsername());
        result.put("previousStatus", previousStatus.name());
        result.put("status", newStatus.name());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 变更用户状态",
            BizLogHelper.trace(),
            BizLogHelper.params("userId", userId, "status", status),
            BizLogHelper.result(BizLogHelper.statusChanged(previousStatus.name(), newStatus.name())),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 禁用用户并记录禁用原因。
     *
     * @param userId 用户 ID
     * @param reason 禁用原因
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableUser(Long userId, String reason) {
        long _start = System.currentTimeMillis();
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserStatus previousStatus = user.getStatus();
        user.disable(reason);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", user.getId().getValue());
        result.put("username", user.getUsername());
        result.put("previousStatus", previousStatus.name());
        result.put("status", UserStatus.DISABLED.name());
        result.put("disableReason", user.getDisableReason());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 禁用用户",
            BizLogHelper.trace(),
            BizLogHelper.params("userId", userId, "reason", reason),
            BizLogHelper.result(BizLogHelper.statusChanged(previousStatus.name(), UserStatus.DISABLED.name())),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 推荐用户。
     *
     * @param userId 用户 ID
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> featureUser(Long userId) {
        long _start = System.currentTimeMillis();
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        user.setRecommended(true);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId().getValue());
        result.put("username", user.getUsername());
        result.put("recommended", true);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 推荐用户",
            BizLogHelper.trace(),
            BizLogHelper.params("userId", userId),
            BizLogHelper.result("推荐成功"),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 取消推荐用户。
     *
     * @param userId 用户 ID
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> unfeatureUser(Long userId) {
        long _start = System.currentTimeMillis();
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));

        user.setRecommended(false);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId().getValue());
        result.put("username", user.getUsername());
        result.put("recommended", false);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 取消推荐用户",
            BizLogHelper.trace(),
            BizLogHelper.params("userId", userId),
            BizLogHelper.result("取消推荐成功"),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 分页查询后台文章列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 文章分页结果
     */
    public PageResult<Map<String, Object>> getArticles(
            int page,
            int pageSize,
            String status,
            String keyword,
            String category) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedCategory = normalizeKeyword(category);
        List<Article> articles = articleRepository.findAdminPage(
            status,
            normalizedKeyword,
            normalizedCategory,
            page,
            pageSize
        );
        long total = articleRepository.countAdminPage(status, normalizedKeyword, normalizedCategory);
        Map<Long, User> authorMap = buildAuthorMap(articles);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(articles.size());
        for (Article article : articles) {
            User author = authorMap.get(article.getAuthorId().getValue());
            Map<String, Object> map = new HashMap<>();
            map.put("id", article.getId().getValue());
            map.put("title", article.getTitle());
            map.put("category", article.getCategory());
            map.put("status", article.getStatus().name());
            map.put("offlineReason", article.getOfflineReason());
            map.put("warnFlag", article.isWarnFlag());
            map.put("viewCount", article.getViewCount());
            map.put("likeCount", article.getLikeCount());
            map.put("favoriteCount", article.getFavoriteCount());
            map.put("commentCount", article.getCommentCount());
            map.put("featured", article.isFeatured());
            map.put("featuredAt", article.getFeaturedAt());
            map.put("scheduledPublishAt", article.getScheduledPublishAt());
            map.put("publishedAt", article.getPublishedAt());
            map.put("createdAt", article.getCreatedAt());
            map.put("updatedAt", article.getUpdatedAt());
            map.put("authorId", article.getAuthorId().getValue());
            map.put("authorUsername", author == null ? null : author.getUsername());
            map.put("authorNickname", author == null ? null : author.getNickname());
            items.add(map);
        }
        return new PageResult<Map<String, Object>>(items, page, pageSize, total);
    }

    /**
     * 更新文章状态。
     *
     * @param articleId 文章 ID
     * @param status 目标状态
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateArticleStatus(Long articleId, String status) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleStatus newStatus = ArticleStatus.valueOf(status);
        ArticleStatus previousStatus = article.getStatus();
        if (ArticleStatus.PUBLISHED.equals(newStatus)) {
            sanitizeArticleBeforeAdminPublish(article);
        }
        applyArticleStatus(article, newStatus);
        articleRepository.save(article);

        if (ArticleStatus.PUBLISHED.equals(newStatus) && previousStatus != ArticleStatus.PUBLISHED) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                article.getId().getValue(), article.getAuthorId().getValue()));
        }
        evictArticlePortalCaches(article, previousStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("id", article.getId().getValue());
        result.put("title", article.getTitle());
        result.put("previousStatus", previousStatus.name());
        result.put("status", newStatus.name());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 变更文章状态",
            BizLogHelper.trace(),
            BizLogHelper.params("articleId", articleId, "status", status),
            BizLogHelper.result(BizLogHelper.statusChanged(previousStatus.name(), newStatus.name())),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 下架文章并记录下架原因。
     *
     * @param articleId 文章 ID
     * @param reason 下架原因
     * @return 变更结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> offlineArticle(Long articleId, String reason) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleStatus previousStatus = article.getStatus();
        article.offline(reason);
        articleRepository.save(article);
        evictArticlePortalCaches(article, previousStatus);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", article.getId().getValue());
        result.put("title", article.getTitle());
        result.put("previousStatus", previousStatus.name());
        result.put("status", ArticleStatus.OFFLINE.name());
        result.put("offlineReason", article.getOfflineReason());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 下架文章",
            BizLogHelper.trace(),
            BizLogHelper.params("articleId", articleId, "reason", reason),
            BizLogHelper.result(BizLogHelper.statusChanged(previousStatus.name(), ArticleStatus.OFFLINE.name())),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 删除文章。
     *
     * @param articleId 文章 ID
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteArticle(Long articleId) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ArticleStatus previousStatus = article.getStatus();
        article.delete();
        articleRepository.save(article);
        evictArticlePortalCaches(article, previousStatus);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", article.getId().getValue());
        result.put("title", article.getTitle());
        result.put("previousStatus", previousStatus.name());
        result.put("status", ArticleStatus.DELETED.name());
        result.put("deleted", true);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 删除文章",
            BizLogHelper.trace(),
            BizLogHelper.params("articleId", articleId),
            BizLogHelper.result(BizLogHelper.statusChanged(previousStatus.name(), ArticleStatus.DELETED.name())),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 批量更新文章状态。
     *
     * @param articleIds 文章 ID 列表
     * @param status 目标状态
     * @return 批量操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchUpdateArticleStatus(List<Long> articleIds, String status) {
        long _start = System.currentTimeMillis();
        if (articleIds == null || articleIds.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请选择至少一篇文章");
        }
        ArticleStatus newStatus = ArticleStatus.valueOf(status);
        int processedCount = 0;
        List<Long> processedIds = new ArrayList<Long>();
        boolean statsCacheInvalidationNeeded = false;
        boolean featuredCacheInvalidationNeeded = false;
        for (Long articleId : articleIds) {
            if (articleId == null) {
                continue;
            }
            Article article = articleRepository.findById(new ArticleId(articleId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
            if (ArticleStatus.DELETED.equals(article.getStatus())) {
                continue;
            }
            if (ArticleStatus.PUBLISHED.equals(newStatus)) {
                sanitizeArticleBeforeAdminPublish(article);
            }
            ArticleStatus previousStatus = article.getStatus();
            applyArticleStatus(article, newStatus);
            articleRepository.save(article);
            statsCacheInvalidationNeeded = statsCacheInvalidationNeeded
                || isPublishVisibilityChanged(previousStatus, article.getStatus());
            featuredCacheInvalidationNeeded = featuredCacheInvalidationNeeded
                || isFeaturedVisibilityChanged(article, previousStatus);
            processedCount++;
            processedIds.add(articleId);
        }
        evictArticlePortalCaches(statsCacheInvalidationNeeded, featuredCacheInvalidationNeeded);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", newStatus.name());
        result.put("processedCount", processedCount);
        result.put("ids", processedIds);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 批量变更文章状态",
            BizLogHelper.trace(),
            BizLogHelper.params("articleCount", articleIds.size(), "status", status),
            BizLogHelper.result("processedCount=" + processedCount),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 批量删除文章。
     *
     * @param articleIds 文章 ID 列表
     * @return 批量操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDeleteArticles(List<Long> articleIds) {
        long _start = System.currentTimeMillis();
        if (articleIds == null || articleIds.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请选择至少一篇文章");
        }
        int processedCount = 0;
        List<Long> processedIds = new ArrayList<Long>();
        boolean statsCacheInvalidationNeeded = false;
        boolean featuredCacheInvalidationNeeded = false;
        for (Long articleId : articleIds) {
            if (articleId == null) {
                continue;
            }
            Article article = articleRepository.findById(new ArticleId(articleId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
            if (ArticleStatus.DELETED.equals(article.getStatus())) {
                continue;
            }
            ArticleStatus previousStatus = article.getStatus();
            article.delete();
            articleRepository.save(article);
            statsCacheInvalidationNeeded = statsCacheInvalidationNeeded
                || isPublishVisibilityChanged(previousStatus, article.getStatus());
            featuredCacheInvalidationNeeded = featuredCacheInvalidationNeeded
                || isFeaturedVisibilityChanged(article, previousStatus);
            processedCount++;
            processedIds.add(articleId);
        }
        evictArticlePortalCaches(statsCacheInvalidationNeeded, featuredCacheInvalidationNeeded);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("deleted", true);
        result.put("processedCount", processedCount);
        result.put("ids", processedIds);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 批量删除文章",
            BizLogHelper.trace(),
            BizLogHelper.params("articleCount", articleIds.size()),
            BizLogHelper.result("processedCount=" + processedCount),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 分页查询后台评论列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param articleId 文章筛选
     * @param keyword 关键字
     * @return 评论分页结果
     */
    public PageResult<Map<String, Object>> getComments(int page, int pageSize, Long articleId, String status, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStatus = normalizeKeyword(status);
        List<Comment> comments = commentRepository.findAdminPage(articleId, normalizedStatus, normalizedKeyword, page, pageSize);
        long total = commentRepository.countAdminPage(articleId, normalizedStatus, normalizedKeyword);
        Map<Long, Article> articleMap = buildArticleMap(comments);
        Map<Long, User> userMap = buildUserMap(comments);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(comments.size());
        for (Comment comment : comments) {
            Article article = articleMap.get(comment.getArticleId().getValue());
            User user = userMap.get(comment.getUserId().getValue());
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId().getValue());
            map.put("articleId", comment.getArticleId().getValue());
            map.put("userId", comment.getUserId().getValue());
            map.put("articleTitle", article == null ? null : article.getTitle());
            map.put("username", user == null ? null : user.getUsername());
            map.put("nickname", user == null ? null : user.getNickname());
            map.put("content", comment.getContent());
            map.put("status", comment.getStatus().name());
            map.put("parentId", comment.getParentId());
            map.put("rootCommentId", comment.getRootCommentId());
            map.put("type", comment.isRootComment() ? "ROOT" : "REPLY");
            map.put("likeCount", comment.getLikeCount());
            map.put("createdAt", comment.getCreatedAt());
            items.add(map);
        }
        return new PageResult<Map<String, Object>>(items, page, pageSize, total);
    }

    /**
     * 删除评论。
     *
     * @param commentId 评论 ID
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteComment(Long commentId) {
        long _start = System.currentTimeMillis();
        Comment comment = commentRepository.findById(new CommentId(commentId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));

        comment.delete();
        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("id", comment.getId().getValue());
        result.put("articleId", comment.getArticleId().getValue());
        result.put("userId", comment.getUserId().getValue());
        result.put("content", comment.getContent());
        result.put("deleted", true);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 删除评论",
            BizLogHelper.trace(),
            BizLogHelper.params("commentId", commentId),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 审核通过评论。
     *
     * @param commentId 评论 ID
     * @return 审核后的评论信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveComment(Long commentId) {
        long _start = System.currentTimeMillis();
        com.myblog.application.dto.CommentDTO dto = commentAppService.approveComment(commentId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", dto.getId());
        result.put("articleId", dto.getArticleId());
        result.put("content", dto.getContent());
        result.put("status", dto.getStatus());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 审核通过评论",
            BizLogHelper.trace(),
            BizLogHelper.params("commentId", commentId),
            BizLogHelper.result("approved=true"),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 拒绝评论。
     *
     * @param commentId 评论 ID
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rejectComment(Long commentId) {
        long _start = System.currentTimeMillis();
        commentAppService.rejectComment(commentId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", commentId);
        result.put("rejected", true);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 拒绝评论",
            BizLogHelper.trace(),
            BizLogHelper.params("commentId", commentId),
            BizLogHelper.result("rejected=true"),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 判断时间是否为指定日期。
     *
     * @param value 时间
     * @param targetDate 目标日期
     * @return 是否匹配
     */
    private boolean isSameDate(LocalDateTime value, LocalDate targetDate) {
        return value != null && targetDate != null && targetDate.equals(value.toLocalDate());
    }

    /**
     * 判断时间是否位于最近区间。
     *
     * @param value 时间
     * @param startDate 起始日期
     * @return 是否位于区间
     */
    private boolean isWithinLastDays(LocalDateTime value, LocalDate startDate) {
        return value != null && startDate != null && !value.toLocalDate().isBefore(startDate);
    }

    /**
     * 规范化搜索关键字。
     *
     * @param keyword 原始关键字
     * @return 规范化结果
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }

    /**
     * 判断用户是否匹配关键字。
     *
     * @param user 用户
     * @param keyword 关键字
     * @return 是否匹配
     */
    private boolean matchesUserKeyword(User user, String keyword) {
        if (keyword == null) {
            return true;
        }
        return containsIgnoreCase(user.getUsername(), keyword)
            || containsIgnoreCase(user.getNickname(), keyword)
            || containsIgnoreCase(user.getEmail().getValue(), keyword);
    }

    /**
     * 忽略大小写匹配文本。
     *
     * @param value 原始值
     * @param keyword 关键字
     * @return 是否匹配
     */
    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null
            && keyword != null
            && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private void applyArticleStatus(Article article, ArticleStatus status) {
        if (ArticleStatus.PUBLISHED.equals(status)) {
            article.publish();
            return;
        }
        if (ArticleStatus.OFFLINE.equals(status)) {
            article.offline();
            return;
        }
        if (ArticleStatus.DRAFT.equals(status)) {
            article.saveDraft();
            return;
        }
        if (ArticleStatus.DELETED.equals(status)) {
            article.delete();
            return;
        }
        article.updateStatus(status);
    }

    private void evictArticlePortalCaches(Article article, ArticleStatus previousStatus) {
        evictArticlePortalCaches(
            isPublishVisibilityChanged(previousStatus, article.getStatus()),
            isFeaturedVisibilityChanged(article, previousStatus)
        );
    }

    private void evictArticlePortalCaches(boolean statsCacheInvalidationNeeded,
                                          boolean featuredCacheInvalidationNeeded) {
        if (featuredCacheInvalidationNeeded) {
            homePortalCacheInvalidator.evictFeaturedAndBootstrap();
        }
        if (statsCacheInvalidationNeeded) {
            homePortalCacheInvalidator.evictStatsAndBootstrap();
        }
    }

    private boolean isPublishVisibilityChanged(ArticleStatus previousStatus, ArticleStatus currentStatus) {
        return ArticleStatus.PUBLISHED.equals(previousStatus) != ArticleStatus.PUBLISHED.equals(currentStatus);
    }

    private boolean isFeaturedVisibilityChanged(Article article, ArticleStatus previousStatus) {
        return article.isFeatured() && isPublishVisibilityChanged(previousStatus, article.getStatus());
    }

    private void sanitizeArticleBeforeAdminPublish(Article article) {
        String sensitiveText = buildArticleSensitiveText(article);
        List<String> blockHits = sensitiveWordAppService.detectBlockWords(sensitiveText);
        if (!blockHits.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR,
                "发布文章失败：内容包含被禁止的敏感词（" + String.join("、", blockHits) + "），请修改后重试");
        }
        List<String> warnHits = sensitiveWordAppService.detectWarnWords(sensitiveText);
        article.updateContent(
            sensitiveWordAppService.maskWarnWords(article.getTitle()),
            sensitiveWordAppService.maskWarnWords(article.getSummary()),
            sensitiveWordAppService.maskWarnWords(article.getContent()),
            article.getCoverUrl(),
            article.getCategory(),
            article.getTags(),
            article.getSlug(),
            article.getSeoTitle(),
            article.getSeoDescription()
        );
        article.updateWarnFlag(!warnHits.isEmpty());
    }

    private String buildArticleSensitiveText(Article article) {
        return normalizeText(article.getTitle()) + "\n"
            + normalizeText(article.getSummary()) + "\n"
            + normalizeText(article.getContent());
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<Long, User> buildAuthorMap(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> authorIds = articles.stream()
            .map(article -> article.getAuthorId().getValue())
            .distinct()
            .collect(Collectors.toList());
        List<User> users = userRepository.findByIds(authorIds);
        Map<Long, User> authorMap = new HashMap<Long, User>(users.size());
        for (User user : users) {
            authorMap.put(user.getId().getValue(), user);
        }
        return authorMap;
    }

    /**
     * 构建文章映射，避免评论后台列表 N+1 查询。
     *
     * @param comments 评论列表
     * @return 文章映射
     */
    private Map<Long, Article> buildArticleMap(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> articleIds = comments.stream()
            .map(comment -> comment.getArticleId().getValue())
            .distinct()
            .collect(Collectors.toList());
        List<Article> articles = articleRepository.findByIds(articleIds);
        Map<Long, Article> articleMap = new HashMap<Long, Article>(articles.size());
        for (Article article : articles) {
            articleMap.put(article.getId().getValue(), article);
        }
        return articleMap;
    }

    /**
     * 设为精选（带权重）。
     *
     * @param articleId 文章 ID
     * @param weight    精选权重 0-1000000，值越大排序越靠前，null 时取默认值 500
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> featureArticle(Long articleId, Integer weight) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        int effectiveWeight = (weight == null) ? 500 : weight;
        article.feature(effectiveWeight);
        articleRepository.save(article);
        homePortalCacheInvalidator.evictFeaturedAndBootstrap();

        Map<String, Object> result = new HashMap<>();
        result.put("id", article.getId().getValue());
        result.put("title", article.getTitle());
        result.put("featured", article.isFeatured());
        result.put("featureWeight", article.getFeatureWeight());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 设为精选",
            BizLogHelper.trace(),
            BizLogHelper.params("articleId", articleId, "title", article.getTitle(), "weight", effectiveWeight),
            BizLogHelper.result("featured=true, featureWeight=" + article.getFeatureWeight()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 取消精选。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> unfeatureArticle(Long articleId) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        article.unfeature();
        articleRepository.save(article);
        homePortalCacheInvalidator.evictFeaturedAndBootstrap();

        Map<String, Object> result = new HashMap<>();
        result.put("id", article.getId().getValue());
        result.put("title", article.getTitle());
        result.put("featured", article.isFeatured());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "管理员 取消精选",
            BizLogHelper.trace(),
            BizLogHelper.params("articleId", articleId, "title", article.getTitle()),
            BizLogHelper.result("featured=false"),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 构建用户映射，避免评论后台列表 N+1 查询。
     *
     * @param comments 评论列表
     * @return 用户映射
     */
    private Map<Long, User> buildUserMap(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = comments.stream()
            .map(comment -> comment.getUserId().getValue())
            .distinct()
            .collect(Collectors.toList());
        List<User> users = userRepository.findByIds(userIds);
        Map<Long, User> userMap = new HashMap<Long, User>(users.size());
        for (User user : users) {
            userMap.put(user.getId().getValue(), user);
        }
        return userMap;
    }

    /**
     * 导出所有用户为 CSV 字节数组（最多 50000 行）。
     *
     * @return CSV 字节数组
     */
    public byte[] exportUsersCsv(String status, String keyword) {
        List<User> users = userRepository.findAdminPage(status, keyword, 1, 50000);
        StringBuilder sb = new StringBuilder();
        sb.append("id,username,email,nickname,role,status,createdAt\n");
        for (User u : users) {
            sb.append(escapeCsv(String.valueOf(u.getId().getValue()))).append(",");
            sb.append(escapeCsv(u.getUsername())).append(",");
            sb.append(escapeCsv(u.getEmail() != null ? u.getEmail().getValue() : "")).append(",");
            sb.append(escapeCsv(u.getNickname())).append(",");
            sb.append(escapeCsv(u.getRole() != null ? u.getRole().name() : "")).append(",");
            sb.append(escapeCsv(u.getStatus() != null ? u.getStatus().name() : "")).append(",");
            sb.append(escapeCsv(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 导出所有文章为 CSV 字节数组（最多 50000 行）。
     *
     * @return CSV 字节数组
     */
    public byte[] exportArticlesCsv(String status, String keyword, String category) {
        List<Article> articles = articleRepository.findAdminPage(status, keyword, category, 1, 50000);
        Map<Long, User> authorMap = buildAuthorMap(articles);
        StringBuilder sb = new StringBuilder();
        sb.append("id,title,category,status,authorUsername,viewCount,likeCount,commentCount,publishedAt,createdAt\n");
        for (Article a : articles) {
            User author = authorMap.get(a.getAuthorId().getValue());
            sb.append(escapeCsv(String.valueOf(a.getId().getValue()))).append(",");
            sb.append(escapeCsv(a.getTitle())).append(",");
            sb.append(escapeCsv(a.getCategory())).append(",");
            sb.append(escapeCsv(a.getStatus() != null ? a.getStatus().name() : "")).append(",");
            sb.append(escapeCsv(author != null ? author.getUsername() : "")).append(",");
            sb.append(a.getViewCount()).append(",");
            sb.append(a.getLikeCount()).append(",");
            sb.append(a.getCommentCount()).append(",");
            sb.append(escapeCsv(a.getPublishedAt() != null ? a.getPublishedAt().toString() : "")).append(",");
            sb.append(escapeCsv(a.getCreatedAt() != null ? a.getCreatedAt().toString() : "")).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
