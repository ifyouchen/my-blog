package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.infrastructure.repository.persistence.converter.ArticlePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ArticleDO;
import com.myblog.infrastructure.repository.persistence.entity.ArticleTagRowDO;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleMetricsDO;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO;
import com.myblog.infrastructure.repository.persistence.entity.DashboardTrendPointDO;
import com.myblog.infrastructure.repository.persistence.mapper.ArticleMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文章 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisArticleRepository implements ArticleRepository {

    private final ArticleMapper articleMapper;
    private final IdGenerator idGenerator;

    /**
     * 创建文章 MyBatis 仓储。
     *
     * @param articleMapper 文章 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisArticleRepository(ArticleMapper articleMapper, IdGenerator idGenerator) {
        this.articleMapper = articleMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据文章 ID 查询文章。
     *
     * @param articleId 文章 ID
     * @return 文章 Optional
     */
    @Override
    public Optional<Article> findById(ArticleId articleId) {
        ArticleDO articleDO = articleMapper.selectById(articleId.getValue());
        if (articleDO == null) {
            return Optional.empty();
        }
        List<String> tags = articleMapper.selectTagNamesByArticleId(articleId.getValue());
        return Optional.of(ArticlePersistenceConverter.toDomain(articleDO, tags));
    }

    /**
     * 查询已发布文章。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     * @return 已发布文章列表
     */
    @Override
    public List<Article> findPublished(String keyword, String category, String tag, String sort) {
        return toDomainList(articleMapper.selectPublished(keyword, category, null, tag, sort, null, null));
    }

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
    @Override
    public List<Article> findPublishedWithLimit(String keyword, String category, String tag, String sort, Integer limit, Integer offset) {
        return toDomainList(articleMapper.selectPublished(keyword, category, null, tag, sort, limit, offset));
    }

    @Override
    public List<Article> findPublishedWithLimit(String keyword, String category, String categoryGroup, String tag, String sort, Integer limit, Integer offset) {
        return toDomainList(articleMapper.selectPublished(keyword, category, categoryGroup, tag, sort, limit, offset));
    }

    /**
     * 查询指定分类和时间范围内的排行文章列表。
     *
     * @param category      分类
     * @param publishedAfter 发布时间下限
     * @param limit         返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findRankingArticles(String category, LocalDateTime publishedAfter, int limit) {
        return toDomainList(articleMapper.selectRankingArticles(category, publishedAfter, limit));
    }

    /**
     * 统计已发布文章数量。
     *
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @return 已发布文章数量
     */
    @Override
    public long countPublished(String keyword, String category, String tag, String sort) {
        return articleMapper.countPublished(keyword, category, null, tag, sort);
    }

    @Override
    public long countPublished(String keyword, String category, String categoryGroup, String tag, String sort) {
        return articleMapper.countPublished(keyword, category, categoryGroup, tag, sort);
    }

    /**
     * 查询作者自己的文章列表。
     *
     * @param authorId 作者 ID
     * @return 作者文章列表
     */
    @Override
    public List<Article> findByAuthorId(Long authorId) {
        return toDomainList(articleMapper.selectByAuthorId(authorId));
    }

    /**
     * 分页查询作者文章列表（含状态和关键字过滤）。
     *
     * @param authorId 作者 ID
     * @param status   文章状态
     * @param keyword  关键字
     * @param page     页码
     * @param pageSize 每页大小
     * @return 文章列表
     */
    @Override
    public List<Article> findByAuthorId(Long authorId, String status, String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        return toDomainList(articleMapper.selectByAuthorIdWithStatus(authorId, status, keyword, offset, currentPageSize));
    }

    /**
     * 统计作者文章数量（含状态和关键字过滤）。
     *
     * @param authorId 作者 ID
     * @param status   文章状态
     * @param keyword  关键字
     * @return 文章数量
     */
    @Override
    public long countByAuthorId(Long authorId, String status, String keyword) {
        return articleMapper.countByAuthorIdWithStatus(authorId, status, keyword);
    }

    /**
     * 查询作者已发布文章列表。
     *
     * @param authorId 作者 ID
     * @return 已发布文章列表
     */
    @Override
    public List<Article> findPublishedByAuthorId(Long authorId) {
        return toDomainList(articleMapper.selectPublishedByAuthorId(authorId));
    }

    /**
     * 查询作者最新的一篇文章。
     *
     * @param authorId 作者 ID
     * @return 最新文章 Optional
     */
    @Override
    public Optional<Article> findLatestByAuthorId(Long authorId) {
        ArticleDO articleDO = articleMapper.selectLatestByAuthorId(authorId);
        if (articleDO == null) {
            return Optional.empty();
        }
        List<String> tags = articleMapper.selectTagNamesByArticleId(articleDO.getId());
        return Optional.of(ArticlePersistenceConverter.toDomain(articleDO, tags));
    }

    /**
     * 查询作者文章汇总指标。
     *
     * @param authorId 作者 ID
     * @param status   文章状态
     * @return 作者文章指标数据对象
     */
    @Override
    public AuthorArticleMetricsDO summarizeByAuthor(Long authorId, String status) {
        return articleMapper.selectAuthorMetrics(authorId, status);
    }

    /**
     * 查询作者热门已发布文章列表。
     *
     * @param authorId 作者 ID
     * @param limit 限制数量
     * @return 热门文章列表
     */
    @Override
    public List<Article> findHotPublishedByAuthorId(Long authorId, int limit) {
        return toDomainList(articleMapper.selectHotPublishedByAuthorId(authorId, limit));
    }

    /**
     * 查询作者表现文章列表。
     *
     * @param authorId 作者 ID
     * @param sort     排序方式
     * @param limit    返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findAuthorPerformance(Long authorId, String sort, int limit) {
        return toDomainList(articleMapper.selectAuthorPerformance(authorId, sort, Math.max(1, limit)));
    }

    /**
     * 查询作者文章趋势数据。
     *
     * @param authorId  作者 ID
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 趋势数据列表
     */
    @Override
    public List<DashboardTrendPointDO> findAuthorTrendPoints(Long authorId, LocalDate startDate, LocalDate endDate) {
        return articleMapper.selectAuthorTrendPoints(authorId, startDate, endDate);
    }

    /**
     * 分页查询多位作者已发布文章列表。
     *
     * @param authorIds 作者 ID 列表
     * @param sort      排序方式
     * @param page      页码
     * @param pageSize  每页大小
     * @return 文章列表
     */
    @Override
    public List<Article> findPublishedByAuthorIds(List<Long> authorIds, String sort, int page, int pageSize) {
        if (authorIds == null || authorIds.isEmpty()) {
            return new ArrayList<Article>();
        }
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        return toDomainList(articleMapper.selectPublishedByAuthorIds(authorIds, sort, offset, currentPageSize));
    }

    /**
     * 统计多位作者已发布文章数量。
     *
     * @param authorIds 作者 ID 列表
     * @param sort      排序方式
     * @return 文章数量
     */
    @Override
    public long countPublishedByAuthorIds(List<Long> authorIds, String sort) {
        if (authorIds == null || authorIds.isEmpty()) {
            return 0L;
        }
        return articleMapper.countPublishedByAuthorIds(authorIds, sort);
    }

    /**
     * 增强搜索已发布文章（支持作者关键字、日期范围、全文索引）。
     *
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      开始日期
     * @param dateTo        结束日期
     * @param limit         返回数量限制
     * @param offset        偏移量
     * @param useFulltext   是否使用全文索引
     * @return 文章列表
     */
    @Override
    public List<Article> findPublishedEnhanced(String keyword, String category, String tag, String sort,
                                               String authorKeyword, String dateFrom, String dateTo,
                                               Integer limit, Integer offset, boolean useFulltext) {
        return toDomainList(articleMapper.selectPublishedEnhanced(
            keyword, category, tag, sort, authorKeyword, dateFrom, dateTo, limit, offset, useFulltext));
    }

    /**
     * 统计增强搜索已发布文章数量。
     *
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      开始日期
     * @param dateTo        结束日期
     * @param useFulltext   是否使用全文索引
     * @return 文章数量
     */
    @Override
    public long countPublishedEnhanced(String keyword, String category, String tag,
                                       String sort, String authorKeyword, String dateFrom, String dateTo,
                                       boolean useFulltext) {
        return articleMapper.countPublishedEnhanced(
            keyword, category, tag, sort, authorKeyword, dateFrom, dateTo, useFulltext);
    }

    /**
     * 分页增强搜索多位作者已发布文章列表。
     *
     * @param authorIds     作者 ID 列表
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      开始日期
     * @param dateTo        结束日期
     * @param page          页码
     * @param pageSize      每页大小
     * @return 文章列表
     */
    @Override
    public List<Article> findPublishedEnhancedByAuthorIds(List<Long> authorIds, String keyword, String category,
                                                         String tag, String sort, String authorKeyword,
                                                         String dateFrom, String dateTo,
                                                         int page, int pageSize) {
        if (authorIds == null || authorIds.isEmpty()) {
            return new ArrayList<Article>();
        }
        int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
        return toDomainList(articleMapper.selectPublishedEnhancedByAuthorIds(
            authorIds, keyword, category, tag, sort, authorKeyword, dateFrom, dateTo, offset, pageSize));
    }

    /**
     * 统计增强搜索多位作者已发布文章数量。
     *
     * @param authorIds     作者 ID 列表
     * @param keyword       关键字
     * @param category      分类
     * @param tag           标签
     * @param sort          排序方式
     * @param authorKeyword 作者关键字
     * @param dateFrom      开始日期
     * @param dateTo        结束日期
     * @return 文章数量
     */
    @Override
    public long countPublishedEnhancedByAuthorIds(List<Long> authorIds, String keyword, String category,
                                                  String tag, String sort, String authorKeyword, String dateFrom,
                                                  String dateTo) {
        if (authorIds == null || authorIds.isEmpty()) {
            return 0L;
        }
        return articleMapper.countPublishedEnhancedByAuthorIds(
            authorIds, keyword, category, tag, sort, authorKeyword, dateFrom, dateTo);
    }

    /**
     * 分页查询后台文章列表。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页大小
     * @return 文章列表
     */
    @Override
    public List<Article> findAdminPage(String status, String keyword, String category, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<ArticleDO> articleDOList = articleMapper.selectAdminPage(
            status,
            keyword,
            category,
            offset,
            currentPageSize
        );
        return toDomainList(articleDOList);
    }

    /**
     * 统计后台文章数量。
     *
     * @param status 状态筛选
     * @param keyword 关键字
     * @return 文章数量
     */
    @Override
    public long countAdminPage(String status, String keyword, String category) {
        return articleMapper.countAdminPage(status, keyword, category);
    }

    /**
     * 根据文章 ID 批量查询文章。
     *
     * @param articleIds 文章 ID 列表
     * @return 文章列表
     */
    @Override
    public List<Article> findByIds(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return new ArrayList<Article>();
        }
        List<ArticleDO> articleDOList = articleMapper.selectByIds(articleIds);
        return toDomainList(articleDOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article save(Article article) {
        ArticleDO articleDO = ArticlePersistenceConverter.toData(article);
        int affectedRows;
        if (articleMapper.countById(article.getId().getValue()) > 0) {
            affectedRows = articleMapper.update(articleDO);
        } else {
            affectedRows = articleMapper.insert(articleDO);
        }
        if (affectedRows <= 0) {
            throw new ApplicationException(ErrorCode.CONFLICT, "文章已被其他操作修改，请刷新后重试");
        }
        saveTags(article);
        return article;
    }

    /**
     * 查询所有文章。
     *
     * @return 文章列表
     */
    @Override
    public List<Article> findAll() {
        List<ArticleDO> articleDOList = articleMapper.selectAll();
        return toDomainList(articleDOList);
    }

    /**
     * 生成下一个文章 ID。
     *
     * @return 文章 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_article");
    }

    /**
     * 统计已发布文章数量。
     *
     * @return 已发布文章数量
     */
    @Override
    public long countPublished() {
        return articleMapper.countPublished();
    }

    /**
     * 统计有至少一篇已发布文章的作者数量。
     *
     * @return 作者去重数量
     */
    @Override
    public long countPublishedAuthors() {
        return articleMapper.countPublishedAuthors();
    }

    /**
     * 统计可见文章数量（已发布或公开）。
     *
     * @return 可见文章数量
     */
    @Override
    public long countVisible() {
        return articleMapper.countVisible();
    }

    /**
     * 统计指定状态文章数量。
     *
     * @param status 文章状态
     * @return 文章数量
     */
    @Override
    public long countByStatus(String status) {
        return articleMapper.countByStatus(status);
    }

    /**
     * 统计指定日期创建的文章数量。
     *
     * @param date 日期
     * @return 文章数量
     */
    @Override
    public long countCreatedOn(LocalDate date) {
        return articleMapper.countCreatedOn(date);
    }

    /**
     * 统计指定日期及以后创建的文章数量。
     *
     * @param date 起始日期
     * @return 文章数量
     */
    @Override
    public long countCreatedSince(LocalDate date) {
        return articleMapper.countCreatedSince(date);
    }

    /**
     * 查询作者文章统计聚合。
     *
     * @param limit 返回数量限制
     * @return 作者文章统计列表
     */
    @Override
    public List<AuthorArticleStatsDO> findAuthorArticleStats(int limit) {
        return articleMapper.selectAuthorArticleStats(limit);
    }

    /**
     * 查询作者文章统计聚合（用于排行，支持分类和时间过滤）。
     *
     * @param limit          返回数量限制
     * @param category       分类
     * @param publishedAfter 发布时间下限
     * @return 作者文章统计列表
     */
    @Override
    public List<AuthorArticleStatsDO> findAuthorArticleStats(int limit, String category, LocalDateTime publishedAfter) {
        return articleMapper.selectAuthorArticleStatsForRanking(limit, category, publishedAfter);
    }

    /**
     * 根据作者 ID 列表批量查询作者文章统计。
     *
     * @param authorIds 作者 ID 列表
     * @return 作者文章统计列表
     */
    @Override
    public List<AuthorArticleStatsDO> findAuthorArticleStatsByAuthorIds(List<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return articleMapper.selectAuthorArticleStatsByAuthorIds(authorIds);
    }

    /**
     * 查询多位作者的排行文章列表。
     *
     * @param authorIds      作者 ID 列表
     * @param category       分类
     * @param publishedAfter 发布时间下限
     * @return 文章列表
     */
    @Override
    public List<Article> findTopRankingArticlesByAuthorIds(List<Long> authorIds,
                                                           String category,
                                                           LocalDateTime publishedAfter) {
        if (authorIds == null || authorIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return toDomainList(articleMapper.selectTopRankingArticlesByAuthorIds(authorIds, category, publishedAfter));
    }

    /**
     * 保存文章标签（先逻辑删除旧标签，再恢复或插入新标签）。
     *
     * @param article 文章聚合根
     */
    private void saveTags(Article article) {
        articleMapper.logicDeleteTagsByArticleId(article.getId().getValue());
        for (String tag : article.getTags()) {
            if (tag == null || tag.trim().length() == 0) {
                continue;
            }
            saveTag(article.getId().getValue(), tag.trim());
        }
    }

    /**
     * 保存单个文章标签（已存在则恢复，不存在则插入）。
     *
     * @param articleId 文章 ID
     * @param tagName   标签名称
     */
    private void saveTag(Long articleId, String tagName) {
        if (articleMapper.countTag(articleId, tagName) > 0) {
            articleMapper.restoreTag(articleId, tagName);
            return;
        }
        articleMapper.insertTag(articleId, tagName);
    }

    /** 原子递增文章点赞数。 */
    @Override
    public void incrementLikeCount(Long articleId) { articleMapper.incrementLikeCount(articleId); }

    /** 原子递减文章点赞数。 */
    @Override
    public void decrementLikeCount(Long articleId) { articleMapper.decrementLikeCount(articleId); }

    /** 原子递增文章收藏数。 */
    @Override
    public void incrementFavoriteCount(Long articleId) { articleMapper.incrementFavoriteCount(articleId); }

    /** 原子递减文章收藏数。 */
    @Override
    public void decrementFavoriteCount(Long articleId) { articleMapper.decrementFavoriteCount(articleId); }

    /** 原子递增文章评论数。 */
    @Override
    public void incrementCommentCount(Long articleId) { articleMapper.incrementCommentCount(articleId); }

    /** 原子递减文章评论数。 */
    @Override
    public void decrementCommentCount(Long articleId, int decrement) { articleMapper.decrementCommentCount(articleId, decrement); }

    /** 原子递增文章阅读数。 */
    @Override
    public void incrementViewCount(Long articleId) { articleMapper.incrementViewCount(articleId); }

    /**
     * 根据 Slug 查询已发布文章。
     *
     * @param slug URL Slug
     * @return 文章 Optional
     */
    @Override
    public Optional<Article> findBySlug(String slug) {
        ArticleDO articleDO = articleMapper.selectBySlug(slug);
        if (articleDO == null) {
            return Optional.empty();
        }
        List<String> tags = articleMapper.selectTagNamesByArticleId(articleDO.getId());
        return Optional.of(ArticlePersistenceConverter.toDomain(articleDO, tags));
    }

    /**
     * 分页查询精选文章。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 精选文章列表
     */
    @Override
    public List<Article> findFeatured(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        return toDomainList(articleMapper.selectFeatured(offset, currentPageSize));
    }

    /**
     * 查询本周必读文章列表。
     *
     * @param excludeArticleIds 排除的文章 ID 列表
     * @param limit             返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findWeeklyPicks(List<Long> excludeArticleIds, int limit) {
        return toDomainList(articleMapper.selectWeeklyPicks(excludeArticleIds, Math.max(limit, 1)));
    }

    /**
     * 统计精选文章数量。
     *
     * @return 精选文章数量
     */
    @Override
    public long countFeatured() {
        return articleMapper.countFeatured();
    }

    /**
     * 查询同分类相关文章列表（排除当前文章，按热度降序）。
     *
     * @param category  分类
     * @param excludeId 排除的文章 ID
     * @param limit     返回数量限制
     * @return 相关文章列表
     */
    @Override
    public List<Article> findRelated(String category, Long excludeId, int limit) {
        int safeLimit = Math.max(1, limit);
        return toDomainList(articleMapper.selectRelated(category, excludeId, safeLimit));
    }

    /**
     * 查询同一专栏中已发布的文章列表。
     *
     * @param articleId 文章 ID
     * @param excludeId 排除的文章 ID
     * @param limit     返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findPublishedInSameColumns(Long articleId, Long excludeId, int limit) {
        return toDomainList(articleMapper.selectPublishedInSameColumns(articleId, excludeId, Math.max(1, limit)));
    }

    /**
     * 查询作者已发布文章列表（排除指定文章）。
     *
     * @param authorId  作者 ID
     * @param excludeId 排除的文章 ID
     * @param limit     返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findPublishedByAuthorIdExcluding(Long authorId, Long excludeId, int limit) {
        return toDomainList(articleMapper.selectPublishedByAuthorIdExcluding(authorId, excludeId, Math.max(1, limit)));
    }

    /**
     * 根据标签和分类信号查询已发布文章列表。
     *
     * @param tags      标签列表
     * @param category  分类
     * @param excludeId 排除的文章 ID
     * @param limit     返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findPublishedBySignals(List<String> tags, String category, Long excludeId, int limit) {
        return toDomainList(articleMapper.selectPublishedBySignals(tags, category, excludeId, Math.max(1, limit)));
    }

    /**
     * 查询热门已发布文章列表（排除指定文章）。
     *
     * @param excludeIds 排除的文章 ID 列表
     * @param limit      返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findPopularPublishedExcluding(List<Long> excludeIds, int limit) {
        return toDomainList(articleMapper.selectPopularPublishedExcluding(excludeIds, Math.max(1, limit)));
    }

    /**
     * 查询到期待发布的文章列表。
     *
     * @param now   当前时间
     * @param limit 返回数量限制
     * @return 文章列表
     */
    @Override
    public List<Article> findDueScheduled(java.time.LocalDateTime now, int limit) {
        return toDomainList(articleMapper.selectDueScheduled(now, Math.max(1, limit)));
    }

    /**
     * 查询每日新增文章趋势数据。
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 每日文章趋势列表
     */
    @Override
    public List<com.myblog.infrastructure.repository.persistence.entity.AdminTrendPointDO> findDailyArticleTrend(
            java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return articleMapper.selectDailyArticleTrend(startDate, endDate);
    }

    /**
     * 查询已发布文章的分类分布统计。
     *
     * @param limit 最多返回的分类数量
     * @return 分类统计列表
     */
    @Override
    public List<com.myblog.infrastructure.repository.persistence.entity.AdminCategoryStatDO> findCategoryStats(int limit) {
        return articleMapper.selectCategoryStats(limit);
    }

    /**
     * 查询单篇文章趋势数据。
     *
     * @param articleId 文章 ID
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 趋势数据列表
     */
    @Override
    public List<com.myblog.infrastructure.repository.persistence.entity.DashboardTrendPointDO> findArticleTrendPoints(
            Long articleId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return articleMapper.selectArticleTrendPoints(articleId, startDate, endDate);
    }

    /**
     * 将 DO 列表转换为领域对象列表（批量加载标签）。
     *
     * @param articleDOList 文章 DO 列表
     * @return 文章领域对象列表
     */
    private List<Article> toDomainList(List<ArticleDO> articleDOList) {
        List<Article> articles = new ArrayList<Article>(articleDOList.size());
        Map<Long, List<String>> tagMap = loadTags(articleDOList);
        for (ArticleDO articleDO : articleDOList) {
            articles.add(ArticlePersistenceConverter.toDomain(articleDO, tagMap.get(articleDO.getId())));
        }
        return articles;
    }

    /**
     * 批量加载文章标签，返回文章 ID 到标签列表的映射。
     *
     * @param articleDOList 文章 DO 列表
     * @return 文章 ID 到标签列表的映射
     */
    private Map<Long, List<String>> loadTags(List<ArticleDO> articleDOList) {
        Map<Long, List<String>> tagMap = new HashMap<Long, List<String>>();
        if (articleDOList == null || articleDOList.isEmpty()) {
            return tagMap;
        }
        List<Long> articleIds = new ArrayList<Long>(articleDOList.size());
        for (ArticleDO articleDO : articleDOList) {
            articleIds.add(articleDO.getId());
        }
        List<ArticleTagRowDO> tagRows = articleMapper.selectTagRowsByArticleIds(articleIds);
        for (ArticleTagRowDO row : tagRows) {
            List<String> tags = tagMap.get(row.getArticleId());
            if (tags == null) {
                tags = new ArrayList<String>();
                tagMap.put(row.getArticleId(), tags);
            }
            tags.add(row.getTagName());
        }
        return tagMap;
    }

    /**
     * 统计待审核文章数量（warn_flag=1 且未删除）。
     *
     * @return 待审核文章数量
     */
    @Override
    public long countByWarnFlag() {
        return articleMapper.countByWarnFlag();
    }

    /**
     * 查询上一篇已发布文章。
     *
     * @param articleId 当前文章 ID
     * @return 上一篇文章 Optional
     */
    @Override
    public java.util.Optional<com.myblog.domain.model.aggregate.Article> findPrevPublished(Long articleId) {
        com.myblog.infrastructure.repository.persistence.entity.ArticleDO prev =
            articleMapper.selectPrevPublished(articleId);
        if (prev == null) {
           return Optional.empty();
        }
        java.util.List<String> tags = articleMapper.selectTagNamesByArticleId(prev.getId());
        return java.util.Optional.of(ArticlePersistenceConverter.toDomain(prev, tags));
    }

    /**
     * 查询下一篇已发布文章。
     *
     * @param articleId 当前文章 ID
     * @return 下一篇文章 Optional
     */
    @Override
    public java.util.Optional<com.myblog.domain.model.aggregate.Article> findNextPublished(Long articleId) {
        com.myblog.infrastructure.repository.persistence.entity.ArticleDO next =
            articleMapper.selectNextPublished(articleId);
        if (next == null) {
           return Optional.empty();
        }
        java.util.List<String> tags = articleMapper.selectTagNamesByArticleId(next.getId());
        return java.util.Optional.of(ArticlePersistenceConverter.toDomain(next, tags));
    }
}
