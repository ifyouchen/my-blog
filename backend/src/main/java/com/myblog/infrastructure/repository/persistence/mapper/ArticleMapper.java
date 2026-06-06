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
                                    @Param("categoryGroup") String categoryGroup,
                                    @Param("tag") String tag,
                                    @Param("sort") String sort,
                                    @Param("limit") Integer limit,
                                    @Param("offset") Integer offset);

    /**
     * 查询排行文章（按分类和发布时间筛选）。
     *
     * @param category      分类
     * @param publishedAfter 发布时间下限
     * @param limit          返回数量限制
     * @return 文章列表
     */
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
                        @Param("categoryGroup") String categoryGroup,
                        @Param("tag") String tag,
                        @Param("sort") String sort);

    /**
     * 查询作者自己的文章列表。
     *
     * @param authorId 作者 ID
     * @return 文章数据对象列表
     */
    List<ArticleDO> selectByAuthorId(@Param("authorId") Long authorId);

    /**
     * 根据作者 ID、状态和关键字分页查询文章列表。
     *
     * @param authorId 作者 ID
     * @param status   文章状态
     * @param keyword  关键字
     * @param offset   偏移量
     * @param limit    限制数量
     * @return 文章列表
     */
    List<ArticleDO> selectByAuthorIdWithStatus(@Param("authorId") Long authorId,
                                               @Param("status") String status,
                                               @Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    /**
     * 根据作者 ID、状态和关键字统计文章数量。
     *
     * @param authorId 作者 ID
     * @param status   文章状态
     * @param keyword  关键字
     * @return 文章数量
     */
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

    /**
     * 查询作者最新发布的一篇文章。
     *
     * @param authorId 作者 ID
     * @return 文章数据对象
     */
    ArticleDO selectLatestByAuthorId(@Param("authorId") Long authorId);

    /**
     * 查询作者文章指标聚合数据。
     *
     * @param authorId 作者 ID
     * @param status   文章状态
     * @return 作者文章指标数据对象
     */
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

    /**
     * 查询作者文章表现排行。
     *
     * @param authorId 作者 ID
     * @param sort     排序方式
     * @param limit    返回数量限制
     * @return 文章列表
     */
    List<ArticleDO> selectAuthorPerformance(@Param("authorId") Long authorId,
                                            @Param("sort") String sort,
                                            @Param("limit") int limit);

    /**
     * 查询作者文章趋势数据。
     *
     * @param authorId  作者 ID
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 趋势数据列表
     */
    List<DashboardTrendPointDO> selectAuthorTrendPoints(@Param("authorId") Long authorId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    /**
     * 根据多个作者 ID 查询已发布文章列表。
     *
     * @param authorIds 作者 ID 列表
     * @param sort      排序方式
     * @param offset    偏移量
     * @param limit     限制数量
     * @return 文章列表
     */
    List<ArticleDO> selectPublishedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                               @Param("sort") String sort,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    /**
     * 根据多个作者 ID 统计已发布文章数量。
     *
     * @param authorIds 作者 ID 列表
     * @param sort      排序方式
     * @return 文章数量
     */
    long countPublishedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                   @Param("sort") String sort);

    /**
     * 增强版已发布文章查询（支持作者关键字、日期范围和全文检索）。
     *
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      起始日期
     * @param dateTo        结束日期
     * @param limit         返回数量限制
     * @param offset        偏移量
     * @param useFulltext   是否使用全文检索
     * @return 文章列表
     */
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

    /**
     * 增强版已发布文章数量统计。
     *
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      起始日期
     * @param dateTo        结束日期
     * @param useFulltext   是否使用全文检索
     * @return 文章数量
     */
    long countPublishedEnhanced(@Param("keyword") String keyword,
                                @Param("category") String category,
                                @Param("tag") String tag,
                                @Param("sort") String sort,
                                @Param("authorKeyword") String authorKeyword,
                                @Param("dateFrom") String dateFrom,
                                @Param("dateTo") String dateTo,
                                @Param("useFulltext") boolean useFulltext);

    /**
     * 根据多个作者 ID 增强版查询已发布文章列表。
     *
     * @param authorIds     作者 ID 列表
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      起始日期
     * @param dateTo        结束日期
     * @param offset        偏移量
     * @param limit         返回数量限制
     * @param useFulltext   是否使用全文检索
     * @return 文章列表
     */
    List<ArticleDO> selectPublishedEnhancedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                                       @Param("keyword") String keyword,
                                                       @Param("category") String category,
                                                       @Param("tag") String tag,
                                                       @Param("sort") String sort,
                                                       @Param("authorKeyword") String authorKeyword,
                                                       @Param("dateFrom") String dateFrom,
                                                       @Param("dateTo") String dateTo,
                                                       @Param("offset") Integer offset,
                                                       @Param("limit") Integer limit,
                                                       @Param("useFulltext") boolean useFulltext);

    /**
     * 根据多个作者 ID 增强版统计已发布文章数量。
     *
     * @param authorIds     作者 ID 列表
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      起始日期
     * @param dateTo        结束日期
     * @param useFulltext   是否使用全文检索
     * @return 文章数量
     */
    long countPublishedEnhancedByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                           @Param("keyword") String keyword,
                                           @Param("category") String category,
                                           @Param("tag") String tag,
                                           @Param("sort") String sort,
                                           @Param("authorKeyword") String authorKeyword,
                                           @Param("dateFrom") String dateFrom,
                                           @Param("dateTo") String dateTo,
                                           @Param("useFulltext") boolean useFulltext);

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

    /**
     * 批量查询多篇文章的标签关联行。
     *
     * @param articleIds 文章 ID 列表
     * @return 文章标签关联行列表
     */
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

    /**
     * 统计可见文章数量（非草稿且未删除）。
     *
     * @return 可见文章数量
     */
    long countVisible();

    /**
     * 根据状态统计文章数量。
     *
     * @param status 文章状态
     * @return 文章数量
     */
    long countByStatus(@Param("status") String status);

    /**
     * 统计指定日期创建的文章数量。
     *
     * @param date 日期
     * @return 文章数量
     */
    long countCreatedOn(@Param("date") LocalDate date);

    /**
     * 统计指定日期及以后创建的文章数量。
     *
     * @param date 起始日期
     * @return 文章数量
     */
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

    /**
     * 查询排行榜文章统计（支持分类和发布时间筛选）。
     *
     * @param limit          返回数量限制
     * @param category       分类
     * @param publishedAfter 发布时间下限
     * @return 作者文章统计列表
     */
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

    /** 插入文章阅读明细。 */
    int insertArticleView(@Param("articleId") Long articleId);

    /** 查询精选文章（featured = 1 且 status = PUBLISHED）。 */
    List<ArticleDO> selectFeatured(@Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    /** 查询本周必读算法文章。 */
    List<ArticleDO> selectWeeklyPicks(@Param("excludeArticleIds") List<Long> excludeArticleIds,
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

    /**
     * 查询同专栏的已发布文章。
     *
     * @param articleId 当前文章 ID
     * @param excludeId 排除的文章 ID
     * @param limit     返回数量限制
     * @return 文章列表
     */
    List<ArticleDO> selectPublishedInSameColumns(@Param("articleId") Long articleId,
                                                 @Param("excludeId") Long excludeId,
                                                 @Param("limit") int limit);

    /**
     * 查询同一作者的其他已发布文章（排除指定文章）。
     *
     * @param authorId  作者 ID
     * @param excludeId 排除的文章 ID
     * @param limit     返回数量限制
     * @return 文章列表
     */
    List<ArticleDO> selectPublishedByAuthorIdExcluding(@Param("authorId") Long authorId,
                                                       @Param("excludeId") Long excludeId,
                                                       @Param("limit") int limit);

    /**
     * 根据标签和分类信号查询已发布文章。
     *
     * @param tags      标签列表
     * @param category  分类
     * @param excludeId 排除的文章 ID
     * @param limit     返回数量限制
     * @return 文章列表
     */
    List<ArticleDO> selectPublishedBySignals(@Param("tags") List<String> tags,
                                             @Param("category") String category,
                                             @Param("excludeId") Long excludeId,
                                             @Param("limit") int limit);

    /**
     * 查询热门已发布文章（排除指定文章列表）。
     *
     * @param excludeIds 排除的文章 ID 列表
     * @param limit      返回数量限制
     * @return 文章列表
     */
    List<ArticleDO> selectPopularPublishedExcluding(@Param("excludeIds") List<Long> excludeIds,
                                                    @Param("limit") int limit);

    /**
     * 根据多个作者 ID 查询排行文章。
     *
     * @param authorIds      作者 ID 列表
     * @param category       分类
     * @param publishedAfter 发布时间下限
     * @return 文章列表
     */
    List<ArticleDO> selectTopRankingArticlesByAuthorIds(@Param("authorIds") List<Long> authorIds,
                                                        @Param("category") String category,
                                                        @Param("publishedAfter") LocalDateTime publishedAfter);

    /**
     * 查询到期的定时发布文章。
     *
     * @param now  当前时间
     * @param limit 返回数量限制
     * @return 文章列表
     */
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

    /**
     * 根据多个作者 ID 查询文章统计聚合。
     *
     * @param authorIds 作者 ID 列表
     * @return 作者文章统计列表
     */
    List<AuthorArticleStatsDO> selectAuthorArticleStatsByAuthorIds(@Param("authorIds") List<Long> authorIds);
}


