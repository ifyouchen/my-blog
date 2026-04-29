package com.myblog.application.service;

import com.myblog.application.dto.DashboardArticlePerformanceDTO;
import com.myblog.application.dto.DashboardOverviewDTO;
import com.myblog.application.dto.DashboardTrendPointDTO;
import com.myblog.application.dto.NotificationDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleMetricsDO;
import com.myblog.infrastructure.repository.persistence.entity.DashboardTrendPointDO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final ArticleRepository articleRepository;
    private final UserFollowRepository userFollowRepository;
    private final NotificationAppService notificationAppService;

    public DashboardAppService(ArticleRepository articleRepository,
                               UserFollowRepository userFollowRepository,
                               NotificationAppService notificationAppService) {
        this.articleRepository = articleRepository;
        this.userFollowRepository = userFollowRepository;
        this.notificationAppService = notificationAppService;
    }

    public DashboardOverviewDTO getOverview(Long userId) {
        AuthorArticleMetricsDO metrics = articleRepository.summarizeByAuthor(userId, null);
        Article latestArticle = articleRepository.findLatestByAuthorId(userId).orElse(null);
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
        overview.setFollowerCount(userFollowRepository.countFollowers(new UserId(userId)));
        if (latestArticle != null) {
            overview.setLatestArticleId(latestArticle.getId().getValue());
            overview.setLatestArticleTitle(latestArticle.getTitle());
            overview.setLatestArticleStatus(latestArticle.getStatus().name());
            overview.setLatestUpdatedAt(formatDateTime(latestArticle.getUpdatedAt()));
        }
        populateRecommendedAction(overview);
        return overview;
    }

    public List<DashboardTrendPointDTO> getTrends(Long userId, String range) {
        int days = "30d".equalsIgnoreCase(range) ? 30 : 7;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        List<DashboardTrendPointDO> rows = articleRepository.findAuthorTrendPoints(userId, startDate, endDate);
        Map<LocalDate, DashboardTrendPointDO> rowMap = new HashMap<LocalDate, DashboardTrendPointDO>(rows.size());
        for (DashboardTrendPointDO row : rows) {
            rowMap.put(row.getStatDate(), row);
        }

        List<DashboardTrendPointDTO> points = new ArrayList<DashboardTrendPointDTO>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DashboardTrendPointDO row = rowMap.get(date);
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

    public List<NotificationDTO> getInteractions(Long userId) {
        return notificationAppService.listRecent(userId, "all", 8);
    }

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

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : DATETIME_FORMATTER.format(dateTime);
    }

    private int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private long safeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

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
