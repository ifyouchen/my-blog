package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ArticleDO;
import com.myblog.infrastructure.repository.persistence.entity.ArticleTagRowDO;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleMetricsDO;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO;
import com.myblog.infrastructure.repository.persistence.entity.DashboardTrendPointDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ArticleMapper {

    /**
     * 根据 ID 查询文章。
     *
     * @param id 文章 ID
     * @return 文章数据对象
     */
    ArticleDO selectById(@Param("id") Long id);

    /**
     * 查询所有文章。
     *
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectAll();

    /**
     * 分页查询后台文章列表。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文章列表
     */
    List<ArticleDO> selectAdminPage(@Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("category") String category,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 统计后台文章数量。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 文章数量
     */
    long countAdminPage(@Param("status") String status,
                        @Param("keyword") String keyword,
                        @Param("category") String category);

    /**
     * 根据文章 ID 批量查询文章。
     *
     * @param articleIds 文章 ID 列表
     * @return 文章列表
     */
    List<ArticleDO> selectByIds(@Param("articleIds") List<Long> articleIds);

    /**
     * 查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     * @param limit 返回数量限制
     * @param offset 偏移量
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectPublished(@Param("keyword") String keyword,
                                    @Param("category") String category,
                                    @Param("tag") String tag,
                                    @Param("sort") String sort,
                                    @Param("limit") Integer limit,
                                    @Param("offset") Integer offset);

    List<ArticleDO> selectRankingArticles(@Param("category") String category,
                                          @Param("publishedAfter") LocalDateTime publishedAfter,
                                          @Param("limit") int limit);

    /**
     * 统计已发布文章数量。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @return 已发布文章数量
     */
    long countPublished(@Param("keyword") String keyword,
                        @Param("category") String category,
                        @Param("tag") String tag,
                        @Param("sort") String sort);

    /**
     * 查询作者自己的文章列表。
     *
     * @param authorId 作者 ID
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectByAuthorId(@Param("authorId") Long authorId);

    List<ArticleDO> selectByAuthorIdWithStatus(@Param("authorId") Long authorId,
                                               @Param("status") String status,
                                               @Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    long countByAuthorIdWithStatus(@Param("authorId") Long authorId,
                                   @Param("status") String status,
                                   @Param("keyword") String keyword);

    /**
     * 查询作者已发布文章列表。
     *
     * @param authorId 作者 ID
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectPublishedByAuthorId(@Param("authorId") Long authorId);

    ArticleDO selectLatestByAuthorId(@Param("authorId") Long authorId);

    AuthorArticleMetricsDO selectAuthorMetrics(@Param("authorId") Long authorId, @Param("status") String status);

    /**
     * 查询作者热门已发布文章列表。
     *
     * @param authorId 作者 ID
     * @param limit 限制数量
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectHotPublishedByAuthorId(@Param("authorId") Long authorId,
                                                 @Param("limit") int limit);

    List<ArticleDO> selectAuthorPerformance(@Param("authorId") Long authorId,
                                            @Param("sort") String sort,
                                            @Param("limit") int limit);

    List<DashboardTrendPointDO> selectAuthorTrendPoints(@Param("authorId") Long authorId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    List<ArticleDO> selectPublishedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                               @Param("sort") String sort,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    long countPublishedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                   @Param("sort") String sort);

    List<ArticleDO> selectPublishedEnhanced(@Param("keyword") String keyword,
                                            @Param("category") String category,
                                            @Param("tag") String tag,
                                            @Param("sort") String sort,
                                            @Param("authorKeyword") String authorKeyword,
                                            @Param("dateFrom") String dateFrom,
                                            @Param("dateTo") String dateTo,
                                            @Param("limit") Integer limit,
                                            @Param("offset") Integer offset,
                                            @Param("useFulltext") boolean useFulltext);

    long countPublishedEnhanced(@Param("keyword") String keyword,
                                @Param("category") String category,
                                @Param("tag") String tag,
                                @Param("sort") String sort,
                                @Param("authorKeyword") String authorKeyword,
                                @Param("dateFrom") String dateFrom,
                                @Param("dateTo") String dateTo,
                                @Param("useFulltext") boolean useFulltext);

    List<ArticleDO> selectPublishedEnhancedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                                       @Param("keyword") String keyword,
                                                       @Param("category") String category,
                                                       @Param("tag") String tag,
                                                       @Param("sort") String sort,
                                                       @Param("authorKeyword") String authorKeyword,
                                                       @Param("dateFrom") String dateFrom,
                                                       @Param("dateTo") String dateTo,
                                                       @Param("offset") Integer offset,
                                                       @Param("limit") Integer limit);

    long countPublishedEnhancedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                           @Param("keyword") String keyword,
                                           @Param("category") String category,
                                           @Param("tag") String tag,
                                           @Param("sort") String sort,
                                           @Param("authorKeyword") String authorKeyword,
                                           @Param("dateFrom") String dateFrom,
                                           @Param("dateTo") String dateTo);

    /**
     * 根据 ID 统计文章数量。
     *
     * @param id 文章 ID
     * @return 文章数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个文章 ID。
     *
     * @return 下一个文章 ID
     */
    Long selectNextId();

    /**
     * 插入文章。
     *
     * @param article 文章数据对象
     * @return 影响行数
     */
    int insert(ArticleDO article);

    /**
     * 更新文章。
     *
     * @param article 文章数据对象
     * @return 影响行数
     */
    int update(ArticleDO article);

    /**
     * 插入或更新文章（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param article 文章数据对象
     * @return 影响行数
     */
    int insertOrUpdate(ArticleDO article);

    /**
     * 查询文章标签名称列表。
     *
     * @param articleId 文章 ID
     * @return 标签名称列表
     */
    List<String> selectTagNamesByArticleId(@Param("articleId") Long articleId);

    List<ArticleTagRowDO> selectTagRowsByArticleIds(@Param("articleIds") List<Long> articleIds);

    /**
     * 逻辑删除文章标签。
     *
     * @param articleId 文章 ID
     * @return 影响行数
     */
    int logicDeleteTagsByArticleId(@Param("articleId") Long articleId);

    /**
     * 统计文章标签数量。
     *
     * @param articleId 文章 ID
     * @param tagName 标签名称
     * @return 标签数量
     */
    int countTag(@Param("articleId") Long articleId, @Param("tagName") String tagName);

    /**
     * 恢复文章标签。
     *
     * @param articleId 文章 ID
     * @param tagName 标签名称
     * @return 影响行数
     */
    int restoreTag(@Param("articleId") Long articleId, @Param("tagName") String tagName);

    /**
     * 插入文章标签。
     *
     * @param articleId 文章 ID
     * @param tagName 标签名称
     * @return 影响行数
     */
    int insertTag(@Param("articleId") Long articleId, @Param("tagName") String tagName);

    /**
     * 统计已发布文章数量。
     *
     * @return 已发布文章数量
     */
    long countPublished();

    long countVisible();

    long countByStatus(@Param("status") String status);

    long countCreatedOn(@Param("date") LocalDate date);

    long countCreatedSince(@Param("date") LocalDate date);

    /**
     * 统计有至少一篇已发布文章的作者数量。
     *
     * @return 作者去重数量
     */
    long countPublishedAuthors();

    /**
     * 查询作者文章统计聚合。
     *
     * @param limit 返回数量限制
     * @return 作者文章统计列表
     */
    List<AuthorArticleStatsDO> selectAuthorArticleStats(@Param("limit") int limit);

    List<AuthorArticleStatsDO> selectAuthorArticleStatsForRanking(
            @Param("limit") int limit,
            @Param("category") String category,
            @Param("publishedAfter") LocalDateTime publishedAfter);

    /** 原子递增点赞数（避免并发 Read-Modify-Write 丢失更新）。 */
    int incrementLikeCount(@Param("articleId") Long articleId);

    /** 原子递减点赞数（防止降为负数）。 */
    int decrementLikeCount(@Param("articleId") Long articleId);

    /** 原子递增收藏数。 */
    int incrementFavoriteCount(@Param("articleId") Long articleId);

    /** 原子递减收藏数（防止降为负数）。 */
    int decrementFavoriteCount(@Param("articleId") Long articleId);

    /** 原子递增评论数。 */
    int incrementCommentCount(@Param("articleId") Long articleId);

    /** 原子递减评论数（支持批量，删除楼层时同时减去回复数）。 */
    int decrementCommentCount(@Param("articleId") Long articleId, @Param("decrement") int decrement);

    /** 原子递增阅读数。 */
    int incrementViewCount(@Param("articleId") Long articleId);

    /** 查询精选文章（featured = 1 且 status = PUBLISHED）。 */
    List<ArticleDO> selectFeatured(@Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    /** 查询本周必读算法文章。 */
    List<ArticleDO> selectWeeklyPicks(@Param("excludeArticleId") Long excludeArticleId,
                                      @Param("limit") int limit);

    /**
     * 查询同分类相关文章（排除当前文章，按热度降序）。
     *
     * @param category 分类
     * @param excludeId 排除的文章 ID
     * @param limit 返回数量
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectRelated(@Param("category") String category,
                                  @Param("excludeId") Long excludeId,
                                  @Param("limit") int limit);

    List<ArticleDO> selectPublishedInSameColumns(@Param("articleId") Long articleId,
                                                 @Param("excludeId") Long excludeId,
                                                 @Param("limit") int limit);

    List<ArticleDO> selectPublishedByAuthorIdExcluding(@Param("authorId") Long authorId,
                                                       @Param("excludeId") Long excludeId,
                                                       @Param("limit") int limit);

    List<ArticleDO> selectPublishedBySignals(@Param("tags") List<String> tags,
                                             @Param("category") String category,
                                             @Param("excludeId") Long excludeId,
                                             @Param("limit") int limit);

    List<ArticleDO> selectPopularPublishedExcluding(@Param("excludeIds") List<Long> excludeIds,
                                                    @Param("limit") int limit);

    List<ArticleDO> selectTopRankingArticlesByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                                        @Param("category") String category,
                                                        @Param("publishedAfter") LocalDateTime publishedAfter);

    List<ArticleDO> selectDueScheduled(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 统计待审核文章数量（warn_flag=1 且未删除）。 */
    long countByWarnFlag();

    /** 统计精选文章数量。 */
    long countFeatured();

    /**
     * 根据 Slug 查询文章。
     *
     * @param slug URL Slug
     * @return 文章数据对象
     */
    ArticleDO selectBySlug(@Param("slug") String slug);
    /**
     * 查询指定日期范围内每日新增文章趋势。
     *
     * @param startDate 起始日期（含）
     * @param endDate 结束日期（含）
     * @return 每日文章新增数列表
     */
    List<com.myblog.infrastructure.repository.persistence.entity.AdminTrendPointDO> selectDailyArticleTrend(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询已发布文章的分类分布统计。
     *
     * @param limit 最多返回的分类数量
     * @return 分类统计列表
     */
    List<com.myblog.infrastructure.repository.persistence.entity.AdminCategoryStatDO> selectCategoryStats(
            @Param("limit") int limit);


    /**
     * 查询上一篇已发布文章（ID 严格小于给定 id 的最大值）。
     */
    ArticleDO selectPrevPublished(@Param("id") Long id);

    /**
     * 查询下一篇已发布文章（ID 严格大于给定 id 的最小值）。
     */
    ArticleDO selectNextPublished(@Param("id") Long id);

    /**
     * 查询单篇文章趋势数据。
     *
     * @param articleId 文章 ID
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 趋势数据列表
     */
    List<DashboardTrendPointDO> selectArticleTrendPoints(@Param("articleId") Long articleId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    List<AuthorArticleStatsDO> selectAuthorArticleStatsByAuthorIds(@Param("authorIds") List<Long> authorIds);
}


