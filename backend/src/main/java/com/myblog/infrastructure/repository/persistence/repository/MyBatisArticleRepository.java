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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
@Profile("!memory")
public class MyBatisArticleRepository implements ArticleRepository {

    private final ArticleMapper articleMapper;

    /**
     * 创建文章 MyBatis 仓储。
     *
     * @param articleMapper 文章 Mapper
     */
    public MyBatisArticleRepository(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
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
        return toDomainList(articleMapper.selectPublished(keyword, category, tag, sort, null, null));
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
        return toDomainList(articleMapper.selectPublished(keyword, category, tag, sort, limit, offset));
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
    public long countPublished(String keyword, String category, String tag) {
        return articleMapper.countPublished(keyword, category, tag);
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

    @Override
    public List<Article> findByAuthorId(Long authorId, String status, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        return toDomainList(articleMapper.selectByAuthorIdWithStatus(authorId, status, offset, currentPageSize));
    }

    @Override
    public long countByAuthorId(Long authorId, String status) {
        return articleMapper.countByAuthorIdWithStatus(authorId, status);
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

    @Override
    public Optional<Article> findLatestByAuthorId(Long authorId) {
        ArticleDO articleDO = articleMapper.selectLatestByAuthorId(authorId);
        if (articleDO == null) {
            return Optional.empty();
        }
        List<String> tags = articleMapper.selectTagNamesByArticleId(articleDO.getId());
        return Optional.of(ArticlePersistenceConverter.toDomain(articleDO, tags));
    }

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

    @Override
    public List<Article> findAuthorPerformance(Long authorId, String sort, int limit) {
        return toDomainList(articleMapper.selectAuthorPerformance(authorId, sort, Math.max(1, limit)));
    }

    @Override
    public List<DashboardTrendPointDO> findAuthorTrendPoints(Long authorId, LocalDate startDate, LocalDate endDate) {
        return articleMapper.selectAuthorTrendPoints(authorId, startDate, endDate);
    }

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

    @Override
    public long countPublishedByAuthorIds(List<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return 0L;
        }
        return articleMapper.countPublishedByAuthorIds(authorIds);
    }

    @Override
    public List<Article> findPublishedEnhanced(String keyword, String category, String tag, String sort,
                                               String authorKeyword, String dateFrom, String dateTo,
                                               Integer limit, Integer offset, boolean useFulltext) {
        return toDomainList(articleMapper.selectPublishedEnhanced(
            keyword, category, tag, sort, authorKeyword, dateFrom, dateTo, limit, offset, useFulltext));
    }

    @Override
    public long countPublishedEnhanced(String keyword, String category, String tag,
                                      String authorKeyword, String dateFrom, String dateTo, boolean useFulltext) {
        return articleMapper.countPublishedEnhanced(keyword, category, tag, authorKeyword, dateFrom, dateTo, useFulltext);
    }

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

    @Override
    public long countPublishedEnhancedByAuthorIds(List<Long> authorIds, String keyword, String category,
                                                 String tag, String authorKeyword, String dateFrom, String dateTo) {
        if (authorIds == null || authorIds.isEmpty()) {
            return 0L;
        }
        return articleMapper.countPublishedEnhancedByAuthorIds(
            authorIds, keyword, category, tag, authorKeyword, dateFrom, dateTo);
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
        return articleMapper.selectNextId();
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

    @Override
    public long countVisible() {
        return articleMapper.countVisible();
    }

    @Override
    public long countByStatus(String status) {
        return articleMapper.countByStatus(status);
    }

    @Override
    public long countCreatedOn(LocalDate date) {
        return articleMapper.countCreatedOn(date);
    }

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

    private void saveTags(Article article) {
        articleMapper.logicDeleteTagsByArticleId(article.getId().getValue());
        for (String tag : article.getTags()) {
            if (tag == null || tag.trim().length() == 0) {
                continue;
            }
            saveTag(article.getId().getValue(), tag.trim());
        }
    }

    private void saveTag(Long articleId, String tagName) {
        if (articleMapper.countTag(articleId, tagName) > 0) {
            articleMapper.restoreTag(articleId, tagName);
            return;
        }
        articleMapper.insertTag(articleId, tagName);
    }

    @Override
    public void incrementLikeCount(Long articleId) { articleMapper.incrementLikeCount(articleId); }

    @Override
    public void decrementLikeCount(Long articleId) { articleMapper.decrementLikeCount(articleId); }

    @Override
    public void incrementFavoriteCount(Long articleId) { articleMapper.incrementFavoriteCount(articleId); }

    @Override
    public void decrementFavoriteCount(Long articleId) { articleMapper.decrementFavoriteCount(articleId); }

    @Override
    public void incrementCommentCount(Long articleId) { articleMapper.incrementCommentCount(articleId); }

    @Override
    public void decrementCommentCount(Long articleId, int decrement) { articleMapper.decrementCommentCount(articleId, decrement); }

    @Override
    public void incrementViewCount(Long articleId) { articleMapper.incrementViewCount(articleId); }

    @Override
    public Optional<Article> findBySlug(String slug) {
        ArticleDO articleDO = articleMapper.selectBySlug(slug);
        if (articleDO == null) {
            return Optional.empty();
        }
        List<String> tags = articleMapper.selectTagNamesByArticleId(articleDO.getId());
        return Optional.of(ArticlePersistenceConverter.toDomain(articleDO, tags));
    }

    @Override
    public List<Article> findFeatured(int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        return toDomainList(articleMapper.selectFeatured(offset, currentPageSize));
    }

    @Override
    public long countFeatured() {
        return articleMapper.countFeatured();
    }

    @Override
    public List<Article> findRelated(String category, Long excludeId, int limit) {
        int safeLimit = Math.max(1, limit);
        return toDomainList(articleMapper.selectRelated(category, excludeId, safeLimit));
    }

    @Override
    public List<Article> findDueScheduled(java.time.LocalDateTime now, int limit) {
        return toDomainList(articleMapper.selectDueScheduled(now, Math.max(1, limit)));
    }

    @Override
    public List<com.myblog.infrastructure.repository.persistence.entity.AdminTrendPointDO> findDailyArticleTrend(
            java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return articleMapper.selectDailyArticleTrend(startDate, endDate);
    }

    @Override
    public List<com.myblog.infrastructure.repository.persistence.entity.AdminCategoryStatDO> findCategoryStats(int limit) {
        return articleMapper.selectCategoryStats(limit);
    }

    @Override
    public List<com.myblog.infrastructure.repository.persistence.entity.DashboardTrendPointDO> findArticleTrendPoints(
            Long articleId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return articleMapper.selectArticleTrendPoints(articleId, startDate, endDate);
    }

    private List<Article> toDomainList(List<ArticleDO> articleDOList) {
        List<Article> articles = new ArrayList<Article>(articleDOList.size());
        Map<Long, List<String>> tagMap = loadTags(articleDOList);
        for (ArticleDO articleDO : articleDOList) {
            articles.add(ArticlePersistenceConverter.toDomain(articleDO, tagMap.get(articleDO.getId())));
        }
        return articles;
    }

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

    @Override
    public long countByWarnFlag() {
        return articleMapper.countByWarnFlag();
    }

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
