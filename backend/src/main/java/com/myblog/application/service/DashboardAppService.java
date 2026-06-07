package com.myblog.application.service;

import com.myblog.application.dto.ArticleStatsDTO;
import com.myblog.application.dto.ArticleStatsTrendPointDTO;
import com.myblog.application.dto.DashboardArticlePerformanceDTO;
import com.myblog.application.dto.DashboardOverviewDTO;
import com.myblog.application.dto.DashboardTrendPointDTO;
import com.myblog.application.dto.NotificationDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.readmodel.AuthorArticleMetrics;
import com.myblog.domain.model.readmodel.DashboardTrendPoint;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 创作者工作台应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class DashboardAppService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Logger log = LoggerFactory.getLogger(DashboardAppService.class);

    private final ArticleRepository articleRepository;
    private final UserFollowRepository userFollowRepository;
    private final NotificationAppService notificationAppService;
    private final SearchHistoryAppService searchHistoryAppService;
    private final Executor taskExecutor;

    public DashboardAppService(ArticleRepository articleRepository,
                               UserFollowRepository userFollowRepository,
                               NotificationAppService notificationAppService,
                               SearchHistoryAppService searchHistoryAppService,
                               @Qualifier("taskExecutor") Executor taskExecutor) {
        this.articleRepository = articleRepository;
        this.userFollowRepository = userFollowRepository;
        this.notificationAppService = notificationAppService;
        this.searchHistoryAppService = searchHistoryAppService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 获取创作者工作台概览数据（包含文章各状态数、历史累计指标及推荐行动）。
     *
     * @param userId 用户 ID
     * @return 工作台概览 DTO
     */
    public DashboardOverviewDTO getOverview(Long userId) {
        // Three independent read queries — safe to run in parallel
        CompletableFuture<AuthorArticleMetrics> metricsFuture = CompletableFuture.supplyAsync(
            () -> articleRepository.summarizeByAuthor(userId, null), taskExecutor);
        CompletableFuture<Optional<Article>> latestFuture = CompletableFuture.supplyAsync(
            () -> articleRepository.findLatestByAuthorId(userId), taskExecutor);
        CompletableFuture<Integer> followerFuture = CompletableFuture.supplyAsync(
            () -> userFollowRepository.countFollowers(new UserId(userId)), taskExecutor);

        AuthorArticleMetrics metrics = metricsFuture.join();
        Article latestArticle = latestFuture.join().orElse(null);
        int followerCount = followerFuture.join();

        DashboardOverviewDTO overview = new DashboardOverviewDTO();
        overview.setTotalCount(safeInt(metrics != null ? metrics.getArticleCount() : null));
        overview.setDraftCount(safeInt(metrics != null ? metrics.getDraftCount() : null));
        overview.setPublishedCount(safeInt(metrics != null ? metrics.getPublishedCount() : null));
        overview.setOfflineCount(safeInt(metrics != null ? metrics.getOfflineCount() : null));
        overview.setDeletedCount(safeInt(metrics != null ? metrics.getDeletedCount() : null));
        overview.setTotalViewCount(safeLong(metrics != null ? metrics.getTotalViews() : null));
        overview.setTotalLikeCount(safeLong(metrics != null ? metrics.getTotalLikes() : null));
        overview.setTotalFavoriteCount(safeLong(metrics != null ? metrics.getTotalFavorites() : null));
        overview.setTotalCommentCount(safeLong(metrics != null ? metrics.getTotalComments() : null));
        overview.setFollowerCount(followerCount);
        if (latestArticle != null) {
            overview.setLatestArticleId(latestArticle.getId().getValue());
            overview.setLatestArticleTitle(latestArticle.getTitle());
            overview.setLatestArticleStatus(latestArticle.getStatus().name());
            overview.setLatestUpdatedAt(formatDateTime(latestArticle.getUpdatedAt()));
        }
        populateRecommendedAction(overview);
        return overview;
    }

    /**
     * 获取创作者占位趋势数据（默认最近 7 天，支持8 天 / 30 天切换）。
     *
     * @param userId 用户 ID
     * @param range  时间范围（7d / 30d）
     * @return 趋势数据点列表
     */
    public List<DashboardTrendPointDTO> getTrends(Long userId, String range) {
        int days = "30d".equalsIgnoreCase(range) ? 30 : 7;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        List<DashboardTrendPoint> rows = articleRepository.findAuthorTrendPoints(userId, startDate, endDate);
        Map<LocalDate, DashboardTrendPoint> rowMap = toRowMap(rows);

        List<DashboardTrendPointDTO> points = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DashboardTrendPoint row = rowMap.get(date);
            DashboardTrendPointDTO point = new DashboardTrendPointDTO();
            point.setDate(DATE_FORMATTER.format(date));
            point.setViewCount(safeInt(row != null ? row.getViewCount() : null));
            point.setLikeCount(safeInt(row != null ? row.getLikeCount() : null));
            point.setFavoriteCount(safeInt(row != null ? row.getFavoriteCount() : null));
            point.setCommentCount(safeInt(row != null ? row.getCommentCount() : null));
            point.setInteractionCount(point.getLikeCount() + point.getFavoriteCount() + point.getCommentCount());
            points.add(point);
        }
        return points;
    }

    /**
     * 获取文章表现排行（Top 8）。
     *
     * @param userId 用户 ID
     * @param sort   排序方式（view / like / favorite / comment / updated）
     * @return 文章表现列表
     */
    public List<DashboardArticlePerformanceDTO> getArticlePerformance(Long userId, String sort) {
        String normalizedSort = normalizePerformanceSort(sort);
        List<Article> articles = articleRepository.findAuthorPerformance(userId, normalizedSort, 8);
        List<DashboardArticlePerformanceDTO> result = new ArrayList<DashboardArticlePerformanceDTO>(articles.size());
        for (Article article : articles) {
            DashboardArticlePerformanceDTO dto = new DashboardArticlePerformanceDTO();
            dto.setId(article.getId().getValue());
            dto.setTitle(article.getTitle());
            dto.setStatus(article.getStatus().name());
            dto.setViewCount(article.getViewCount());
            dto.setLikeCount(article.getLikeCount());
            dto.setFavoriteCount(article.getFavoriteCount());
            dto.setCommentCount(article.getCommentCount());
            dto.setUpdatedAt(formatDateTime(article.getUpdatedAt()));
            dto.setPublishedAt(formatDateTime(article.getPublishedAt()));
            result.add(dto);
        }
        return result;
    }

    /**
     * 获取创作者最近互动（内容为最近 8 条通知）。
     *
     * @param userId 用户 ID
     * @return 通知 DTO 列表
     */
    public List<NotificationDTO> getInteractions(Long userId) {
        return notificationAppService.listRecent(userId, "all", 8);
    }

    /**
     * 创作者选题建议。
     *
     * @param userId 当前用户 ID
     * @return 选题建议列表
     */
    public List<Map<String, Object>> getContentOpportunities(Long userId) {
        AuthorArticleMetrics metrics = articleRepository.summarizeByAuthor(userId, null);
        List<String> hotKeywords = searchHistoryAppService.getHotKeywords(6);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        int publishedCount = safeInt(metrics != null ? metrics.getPublishedCount() : null);
        for (String keyword : hotKeywords) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("keyword", keyword);
            item.put("title", "围绕「" + keyword + "」补一篇可复用知识卡");
            item.put("reason", "近期搜索热度较高，适合作为专题或专栏里的入口文章。");
            item.put("type", "SEARCH_DEMAND");
            item.put("priority", publishedCount == 0 ? "HIGH" : "MEDIUM");
            result.add(item);
        }
        if (result.isEmpty()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("keyword", "技术笔记");
            item.put("title", "整理一篇基础知识清单");
            item.put("reason", "先把已有经验沉淀为可检索、可编排的知识资产。");
            item.put("type", "KNOWLEDGE_ASSET");
            item.put("priority", "MEDIUM");
            result.add(item);
        }
        return result;
    }

    /**
     * 获取单篇文章详细统计（含 N 日趋势）。
     *
     * @param userId 当前用户 ID（用于鉴权）
     * @param articleId 文章 ID
     * @param range 时间范围（7d / 30d）
     * @return 文章统计 DTO
     */
    public ArticleStatsDTO getArticleStats(Long userId, Long articleId, String range) {
        Article article = articleRepository.findById(new ArticleId(articleId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!article.getAuthorId().getValue().equals(userId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权查看该文章统计");
        }
        int days = "30d".equalsIgnoreCase(range) ? 30 : 7;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        List<DashboardTrendPoint> rows = articleRepository.findArticleTrendPoints(articleId, startDate, endDate);
        Map<LocalDate, DashboardTrendPoint> rowMap = toRowMap(rows);
        List<ArticleStatsTrendPointDTO> trends = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DashboardTrendPoint row = rowMap.get(date);
            ArticleStatsTrendPointDTO point = new ArticleStatsTrendPointDTO();
            point.setDate(DATE_FORMATTER.format(date));
            point.setViewCount(safeInt(row != null ? row.getViewCount() : null));
            point.setLikeCount(safeInt(row != null ? row.getLikeCount() : null));
            point.setFavoriteCount(safeInt(row != null ? row.getFavoriteCount() : null));
            point.setCommentCount(safeInt(row != null ? row.getCommentCount() : null));
            trends.add(point);
        }
        fillMissingArticleViews(article.getViewCount(), trends);
        ArticleStatsDTO stats = new ArticleStatsDTO();
        stats.setArticleId(article.getId().getValue());
        stats.setTitle(article.getTitle());
        stats.setStatus(article.getStatus().name());
        stats.setViewCount(article.getViewCount());
        stats.setLikeCount(article.getLikeCount());
        stats.setFavoriteCount(article.getFavoriteCount());
        stats.setCommentCount(article.getCommentCount());
        stats.setPublishedAt(formatDateTime(article.getPublishedAt()));
        stats.setUpdatedAt(formatDateTime(article.getUpdatedAt()));
        stats.setTrends(trends);
        return stats;
    }

    /**
     * 补齐早期未写入阅读明细的累计阅读数。
     *
     * @param totalViewCount 文章累计阅读数
     * @param trends         趋势数据
     */
    private void fillMissingArticleViews(int totalViewCount, List<ArticleStatsTrendPointDTO> trends) {
        if (totalViewCount <= 0 || trends == null || trends.isEmpty()) {
            return;
        }
        int trendViewTotal = 0;
        for (ArticleStatsTrendPointDTO point : trends) {
            trendViewTotal += safeInt(point.getViewCount());
        }
        int missingViews = totalViewCount - trendViewTotal;
        if (missingViews <= 0) {
            return;
        }
        ArticleStatsTrendPointDTO lastPoint = trends.get(trends.size() - 1);
        lastPoint.setViewCount(safeInt(lastPoint.getViewCount()) + missingViews);
    }

    /**
     * 规范化文章表现排序方式，无效时回退到阅读量排序。
     *
     * @param sort 原始排序字符串
     * @return 规范化后的排序方式
     */
    private String normalizePerformanceSort(String sort) {
        if ("like".equalsIgnoreCase(sort)) {
            return "like";
        }
        if ("favorite".equalsIgnoreCase(sort)) {
            return "favorite";
        }
        if ("comment".equalsIgnoreCase(sort)) {
            return "comment";
        }
        if ("updated".equalsIgnoreCase(sort)) {
            return "updated";
        }
        return "view";
    }

    /**
     * 格式化日期时间为字符串。
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串，null 时返回 null
     */
    private Map<LocalDate, DashboardTrendPoint> toRowMap(List<DashboardTrendPoint> rows) {
        Map<LocalDate, DashboardTrendPoint> map = new HashMap<>(rows.size());
        for (DashboardTrendPoint row : rows) {
            map.put(row.getStatDate(), row);
        }
        return map;
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : DATETIME_FORMATTER.format(dateTime);
    }

    /**
     * 安全转换为 int，null 时返回 0。
     *
     * @param value 数字值
     * @return int 值
     */
    private int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * 安全转换为 long，null 时返回 0L。
     *
     * @param value 数字值
     * @return long 值
     */
    private long safeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

    /**
     * 根据工作台数据填充推荐行动建议。
     *
     * @param overview 工作台概览 DTO
     */
    private void populateRecommendedAction(DashboardOverviewDTO overview) {
        if (overview.getTotalCount() <= 0) {
            overview.setRecommendedActionType("CREATE");
            overview.setRecommendedActionText("开始写第一篇文章");
            overview.setRecommendedActionHint("先完成一篇草稿，创作台会在这里持续给你下一步建议。");
            overview.setRecommendedActionRoute("/editor/new");
            return;
        }
        if (overview.getDraftCount() > 0) {
            overview.setRecommendedActionType("DRAFT");
            overview.setRecommendedActionText("继续处理草稿");
            overview.setRecommendedActionHint("当前还有 " + overview.getDraftCount() + " 篇草稿待整理，优先补全后再发布。");
            overview.setRecommendedActionRoute("/dashboard/articles?status=DRAFT");
            return;
        }
        if (overview.getOfflineCount() > 0) {
            overview.setRecommendedActionType("OFFLINE");
            overview.setRecommendedActionText("检查已下架内容");
            overview.setRecommendedActionHint("有 " + overview.getOfflineCount() + " 篇文章处于下架状态，可以评估是否重新发布。");
            overview.setRecommendedActionRoute("/dashboard/articles?status=OFFLINE");
            return;
        }
        overview.setRecommendedActionType("PUBLISHED");
        overview.setRecommendedActionText("查看已发布文章");
        overview.setRecommendedActionHint("你的内容已经在线，继续优化标题、摘要和互动表现会更有效。");
        overview.setRecommendedActionRoute("/dashboard/articles?status=PUBLISHED");
    }
}
