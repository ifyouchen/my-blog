package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.readmodel.AdminCategoryStat;
import com.myblog.domain.model.readmodel.AdminTrendPoint;
import com.myblog.domain.model.readmodel.AuthorArticleMetrics;
import com.myblog.domain.model.readmodel.AuthorArticleStats;
import com.myblog.domain.model.readmodel.DashboardTrendPoint;
import com.myblog.domain.model.valueobject.ArticleId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文章仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface ArticleRepository {

    /**
     * 根据文章 ID 查询文章。
     *
     * @param articleId 文章 ID
     * @return 文章 Optional
     */
    Optional<Article> findById(ArticleId articleId);

    /**
     * 查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     * @return 已发布文章列表
     */
    List<Article> findPublished(String keyword, String category, String tag, String sort);

    /**
     * 分页查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     * @param limit 返回数量限制
     * @param offset 偏移量
     * @return 已发布文章列表
     */
    List<Article> findPublishedWithLimit(String keyword, String category, String tag, String sort, Integer limit, Integer offset);

    List<Article> findPublishedWithLimit(String keyword, String category, String categoryGroup, String tag, String sort, Integer limit, Integer offset);

    /**
     * 查询排行榜文章。
     *
     * @param category 分类
     * @param publishedAfter 发布时间下限
     * @param limit 返回数量限制
     * @return 排行榜文章列表
     */
    List<Article> findRankingArticles(String category, LocalDateTime publishedAfter, int limit);

    /**
     * 统计已发布文章数量。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @return 已发布文章数量
     */
    long countPublished(String keyword, String category, String tag, String sort);

    long countPublished(String keyword, String category, String categoryGroup, String tag, String sort);

    /**
     * 查询作者自己的文章列表。
     *
     * @param authorId 作者 ID
     * @return 作者文章列表
     */
    List<Article> findByAuthorId(Long authorId);

    /**
     * 分页查询作者文章列表。
     *
     * @param authorId 作者 ID
     * @param status 状态筛选
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页数量
     * @return 作者文章列表
     */
    List<Article> findByAuthorId(Long authorId, String status, String keyword, int page, int pageSize);

    /**
     * 统计作者文章数量。
     *
     * @param authorId 作者 ID
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 文章数量
     */
    long countByAuthorId(Long authorId, String status, String keyword);

    /**
     * 查询作者已发布文章列表。
     *
     * @param authorId 作者 ID
     * @return 已发布文章列表
     */
    List<Article> findPublishedByAuthorId(Long authorId);

    /**
     * 查询作者最新文章。
     *
     * @param authorId 作者 ID
     * @return 最新文章
     */
    Optional<Article> findLatestByAuthorId(Long authorId);

    /**
     * 查询作者文章聚合数据。
     *
     * @param authorId 作者 ID
     * @param status 状态筛选
     * @return 聚合数据
     */
    AuthorArticleMetrics summarizeByAuthor(Long authorId, String status);

    /**
     * 查询作者热门文章列表。
     *
     * @param authorId 作者 ID
     * @param limit 限制数量
     * @return 热门文章列表
     */
    List<Article> findHotPublishedByAuthorId(Long authorId, int limit);

    /**
     * 查询作者文章表现列表。
     *
     * @param authorId 作者 ID
     * @param sort 排序方式
     * @param limit 限制数量
     * @return 文章列表
     */
    List<Article> findAuthorPerformance(Long authorId, String sort, int limit);

    /**
     * 查询作者趋势聚合。
     *
     * @param authorId 作者 ID
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 趋势聚合列表
     */
    List<DashboardTrendPoint> findAuthorTrendPoints(Long authorId, LocalDate startDate, LocalDate endDate);

    /**
     * 分页查询关注作者的已发布文章。
     *
     * @param authorIds 作者 ID 列表
     * @param sort 排序方式
     * @param page 页码
     * @param pageSize 每页大小
     * @return 文章列表
     */
    List<Article> findPublishedByAuthorIds(List<Long> authorIds, String sort, int page, int pageSize);

    /**
     * 统计关注作者的已发布文章数量。
     *
     * @param authorIds 作者 ID 列表
     * @return 文章数量
     */
    long countPublishedByAuthorIds(List<Long> authorIds, String sort);

    /**
     * 增强分页查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom 起始日期
     * @param dateTo 结束日期
     * @param limit 返回数量限制
     * @param offset 偏移量
     * @return 已发布文章列表
     */
    List<Article> findPublishedEnhanced(String keyword, String category, String tag, String sort,
                                       String authorKeyword, String dateFrom, String dateTo,
                                       Integer limit, Integer offset, boolean useFulltext);

    /**
     * 统计增强查询已发布文章数量。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param authorKeyword 作者关键字
     * @param dateFrom 起始日期
     * @param dateTo 结束日期
     * @return 已发布文章数量
     */
    long countPublishedEnhanced(String keyword, String category, String tag,
                                String sort, String authorKeyword, String dateFrom, String dateTo,
                                boolean useFulltext);

    /**
     * 增强分页查询关注作者的已发布文章。
     *
     * @param authorIds 作者 ID 列表
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom 起始日期
     * @param dateTo 结束日期
     * @param page 页码
     * @param pageSize 每页大小
     * @param useFulltext 是否使用全文检索
     * @return 文章列表
     */
    List<Article> findPublishedEnhancedByAuthorIds(List<Long> authorIds, String keyword, String category,
                                                   String tag, String sort, String authorKeyword,
                                                   String dateFrom, String dateTo,
                                                   int page, int pageSize, boolean useFulltext);

    /**
     * 统计增强查询关注作者的已发布文章数量。
     *
     * @param authorIds 作者 ID 列表
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param authorKeyword 作者关键字
     * @param dateFrom 起始日期
     * @param dateTo 结束日期
     * @param useFulltext 是否使用全文检索
     * @return 文章数量
     */
    long countPublishedEnhancedByAuthorIds(List<Long> authorIds, String keyword, String category,
                                           String tag, String sort, String authorKeyword, String dateFrom,
                                           String dateTo, boolean useFulltext);

    /**
     * 分页查询后台文章列表。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页大小
     * @return 文章列表
     */
    List<Article> findAdminPage(String status, String keyword, String category, int page, int pageSize);

    /**
     * 统计后台文章数量。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 文章数量
     */
    long countAdminPage(String status, String keyword, String category);

    /**
     * 根据文章 ID 批量查询文章。
     *
     * @param articleIds 文章 ID 列表
     * @return 文章列表
     */
    List<Article> findByIds(List<Long> articleIds);

    /**
     * 保存文章。
     *
     * @param article 文章聚合根
     * @return 保存后的文章
     */
    Article save(Article article);

    /**
     * 查询所有文章。
     *
     * @return 文章列表
     */
    List<Article> findAll();

    /**
     * 生成下一个文章 ID。
     *
     * @return 文章 ID
     */
    Long nextId();

    /**
     * 统计已发布文章数量。
     *
     * @return 已发布文章数量
     */
    long countPublished();

    /**
     * 统计有至少一篇已发布文章的作者数量。
     *
     * @return 作者去重数量
     */
    long countPublishedAuthors();

    /**
     * 统计未删除文章数量。
     *
     * @return 文章数量
     */
    long countVisible();

    /**
     * 按状态统计文章数量。
     *
     * @param status 状态
     * @return 文章数量
     */
    long countByStatus(String status);

    /**
     * 统计指定日期创建的文章数量。
     *
     * @param date 日期
     * @return 文章数量
     */
    long countCreatedOn(LocalDate date);

    /**
     * 统计指定日期之后创建的文章数量。
     *
     * @param date 起始日期
     * @return 文章数量
     */
    long countCreatedSince(LocalDate date);

    /**
     * 查询作者文章统计聚合。
     *
     * @param limit 返回数量限制
     * @return 作者文章统计列表
     */
    List<AuthorArticleStats> findAuthorArticleStats(int limit);

    /**
     * 查询作者文章统计聚合（带分类和时间筛选）。
     *
     * @param limit         返回数量限制
     * @param category      分类筛选
     * @param publishedAfter 发布时间下限
     * @return 作者文章统计列表
     */
    List<AuthorArticleStats> findAuthorArticleStats(
            int limit,
            String category,
            LocalDateTime publishedAfter);

    /**
     * 根据作者 ID 列表查询文章统计聚合。
     *
     * @param authorIds 作者 ID 列表
     * @return 作者文章统计列表
     */
    List<AuthorArticleStats> findAuthorArticleStatsByAuthorIds(List<Long> authorIds);

    /**
     * 根据作者 ID 列表查询排行榜文章。
     *
     * @param authorIds      作者 ID 列表
     * @param category       分类筛选
     * @param publishedAfter 发布时间下限
     * @return 排行榜文章列表
     */
    List<Article> findTopRankingArticlesByAuthorIds(List<Long> authorIds,
                                                    String category,
                                                    LocalDateTime publishedAfter);

    /**
     * 原子递增文章点赞数。
     *
     * @param articleId 文章 ID
     */
    void incrementLikeCount(Long articleId);

    /**
     * 原子递减文章点赞数。
     *
     * @param articleId 文章 ID
     */
    void decrementLikeCount(Long articleId);

    /**
     * 原子递增文章收藏数。
     *
     * @param articleId 文章 ID
     */
    void incrementFavoriteCount(Long articleId);

    /**
     * 原子递减文章收藏数。
     *
     * @param articleId 文章 ID
     */
    void decrementFavoriteCount(Long articleId);

    /**
     * 原子递增文章评论数。
     *
     * @param articleId 文章 ID
     */
    void incrementCommentCount(Long articleId);

    /**
     * 原子递减文章评论数（支持批量）。
     *
     * @param articleId 文章 ID
     * @param decrement 递减数量
     */
    void decrementCommentCount(Long articleId, int decrement);

    /**
     * 原子递增文章阅读数。
     *
     * @param articleId 文章 ID
     */
    void incrementViewCount(Long articleId);

    /**
     * 记录文章阅读明细。
     *
     * @param articleId 文章 ID
     */
    void recordArticleView(Long articleId);

    /**
     * 查询精选文章。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 精选文章列表
     */
    List<Article> findFeatured(int page, int pageSize);

    /**
     * 查询本周必读算法文章。
     *
     * @param excludeArticleIds 排除的文章 ID 列表
     * @param limit             返回数量限制
     * @return 本周必读文章列表
     */
    List<Article> findWeeklyPicks(List<Long> excludeArticleIds, int limit);

    /**
     * 根据 Slug 查询文章。
     *
     * @param slug URL Slug
     * @return 文章 Optional
     */
    Optional<Article> findBySlug(String slug);

    /**
     * 统计精选文章数量。
     *
     * @return 精选文章数量
     */
    long countFeatured();

    /**
     * 查询同分类相关文章（排除当前文章，按热度降序）。
     *
     * @param category 分类
     * @param excludeId 排除的文章 ID
     * @param limit 返回数量
     * @return 相关文章列表
     */
    List<Article> findRelated(String category, Long excludeId, int limit);

    /**
     * 查询与指定文章同专栏的已发布文章。
     *
     * @param articleId 当前文章 ID
     * @param excludeId 排除的文章 ID
     * @param limit 返回数量
     * @return 文章列表
     */
    List<Article> findPublishedInSameColumns(Long articleId, Long excludeId, int limit);

    /**
     * 查询同作者已发布文章。
     *
     * @param authorId 作者 ID
     * @param excludeId 排除的文章 ID
     * @param limit 返回数量
     * @return 文章列表
     */
    List<Article> findPublishedByAuthorIdExcluding(Long authorId, Long excludeId, int limit);

    /**
     * 按标签或分类查询相关已发布文章。
     *
     * @param tags 标签列表
     * @param category 分类
     * @param excludeId 排除的文章 ID
     * @param limit 返回数量
     * @return 文章列表
     */
    List<Article> findPublishedBySignals(List<String> tags, String category, Long excludeId, int limit);

    /**
     * 查询热门已发布文章并排除指定文章。
     *
     * @param excludeIds 排除文章 ID 列表
     * @param limit 返回数量
     * @return 文章列表
     */
    List<Article> findPopularPublishedExcluding(List<Long> excludeIds, int limit);

    /**
     * 查询已到计划发布时间的定时文章。
     *
     * @param now 当前时间
     * @param limit 返回数量限制
     * @return 定时文章列表
     */
    List<Article> findDueScheduled(LocalDateTime now, int limit);

    /**
     * 查询指定日期范围内每日新增文章趋势。
     *
     * @param startDate 起始日期（含）
     * @param endDate 结束日期（含）
     * @return 每日文章新增数列表
     */
    List<AdminTrendPoint> findDailyArticleTrend(
            LocalDate startDate, LocalDate endDate);

    /**
     * 查询已发布文章的分类分布统计。
     *
     * @param limit 最多返回的分类数量
     * @return 分类统计列表
     */
    List<AdminCategoryStat> findCategoryStats(int limit);

    /**
     * 统计命中敏感词警告的文章数量。
     *
     * @return 命中警告标记的文章数量
     */
    long countByWarnFlag();
    /**
     * 查询上一篇已发布文章（ID 小于给定值的最大项）。
     *
     * @param articleId 当前文章 ID
     * @return Optional 上一篇
     */
    java.util.Optional<Article> findPrevPublished(Long articleId);

    /**
     * 查询下一篇已发布文章（ID 大于给定值的最小项）。
     *
     * @param articleId 当前文章 ID
     * @return Optional 下一篇
     */
    java.util.Optional<Article> findNextPublished(Long articleId);

    /**
     * 查询单篇文章趋势数据。
     *
     * @param articleId 文章 ID
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 趋势数据列表
     */
    List<DashboardTrendPoint> findArticleTrendPoints(
            Long articleId, LocalDate startDate, LocalDate endDate);
}

