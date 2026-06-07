package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.aggregate.ColumnSubscription;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.LearningPathArticle;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.ColumnSubscriptionRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 专栏应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ColumnAppService {

    private static final Logger log = LoggerFactory.getLogger(ColumnAppService.class);

    private final ColumnRepository columnRepository;
    private final ColumnSubscriptionRepository columnSubscriptionRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ArticleAssembler articleAssembler;
    private final HomePortalCacheInvalidator homePortalCacheInvalidator;
    private final LearningProgressAppService learningProgressAppService;

    public ColumnAppService(ColumnRepository columnRepository,
                            ColumnSubscriptionRepository columnSubscriptionRepository,
                            UserRepository userRepository,
                            ArticleRepository articleRepository,
                            ArticleAssembler articleAssembler,
                            HomePortalCacheInvalidator homePortalCacheInvalidator,
                            LearningProgressAppService learningProgressAppService) {
        this.columnRepository = columnRepository;
        this.columnSubscriptionRepository = columnSubscriptionRepository;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.articleAssembler = articleAssembler;
        this.homePortalCacheInvalidator = homePortalCacheInvalidator;
        this.learningProgressAppService = learningProgressAppService;
    }

    /**
     * 分页查询专栏。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param currentUserId 当前用户 ID
     * @return 专栏分页
     */
    public PageResult<ColumnDTO> pageColumns(int page, int pageSize, Long currentUserId) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<Column> columns = columnRepository.findPublished(currentPage, currentPageSize);
        List<ColumnDTO> items = new ArrayList<ColumnDTO>(columns.size());
        for (Column column : columns) {
            items.add(toDTO(column, currentUserId));
        }
        return new PageResult<ColumnDTO>(items, currentPage, currentPageSize, columnRepository.countPublished());
    }

    /**
     * 分页查询某位作者的公开专栏。
     *
     * @param authorId 作者 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param currentUserId 当前用户 ID
     * @return 作者公开专栏分页
     */
    public PageResult<ColumnDTO> pagePublishedColumnsByAuthor(Long authorId, int page, int pageSize,
                                                              Long currentUserId) {
        userRepository.findById(new UserId(authorId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<Column> columns = columnRepository.findPublishedByAuthorId(authorId, currentPage, currentPageSize);
        List<ColumnDTO> items = new ArrayList<ColumnDTO>(columns.size());
        for (Column column : columns) {
            items.add(toDTO(column, currentUserId));
        }
        return new PageResult<ColumnDTO>(
            items,
            currentPage,
            currentPageSize,
            columnRepository.countPublishedByAuthorId(authorId)
        );
    }

    /**
     * 查询推荐专栏。
     *
     * @param limit 限制数量
     * @param currentUserId 当前用户 ID
     * @return 推荐专栏
     */
    public List<ColumnDTO> listRecommendedColumns(int limit, Long currentUserId) {
        List<Column> columns = columnRepository.findRecommended(limit);
        List<ColumnDTO> items = new ArrayList<ColumnDTO>(columns.size());
        for (Column column : columns) {
            items.add(toDTO(column, currentUserId));
        }
        return items;
    }

    /**
     * 获取专栏详情。
     *
     * @param columnId 专栏 ID
     * @param currentUserId 当前用户 ID
     * @return 专栏详情
     */
    public ColumnDTO getColumnDetail(Long columnId, Long currentUserId) {
        Column column = loadPublishedColumn(columnId);
        ColumnDTO dto = toDTO(column, currentUserId);
        populateLearningPath(dto, currentUserId);
        return dto;
    }

    /**
     * 分页查询专栏文章。
     *
     * @param columnId 专栏 ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 文章分页
     */
    public PageResult<ArticleDTO> pageColumnArticles(Long columnId, int page, int pageSize) {
        loadPublishedColumn(columnId);
        List<LearningPathArticle> relations = columnRepository.findArticleRelations(new ColumnId(columnId));

        // Batch-load all articles in the column (eliminates N individual queries)
        List<Long> allArticleIds = relations.stream()
            .map(LearningPathArticle::getArticleId)
            .collect(Collectors.toList());
        Map<Long, Article> articleMap = articleRepository.findByIds(allArticleIds).stream()
            .collect(Collectors.toMap(a -> a.getId().getValue(), a -> a));

        List<Article> visibleArticles = new ArrayList<>();
        List<Integer> visibleSortOrders = new ArrayList<>();
        for (LearningPathArticle relation : relations) {
            Article article = articleMap.get(relation.getArticleId());
            if (article != null && ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                visibleArticles.add(article);
                visibleSortOrders.add(relation.getStepOrder());
            }
        }

        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, visibleArticles.size());
        int toIndex = Math.min(fromIndex + currentPageSize, visibleArticles.size());
        List<Article> pageArticles = visibleArticles.subList(fromIndex, toIndex);

        // Batch-load authors for only the current page slice
        List<Long> authorIds = pageArticles.stream()
            .map(a -> a.getAuthorId().getValue())
            .distinct()
            .collect(Collectors.toList());
        Map<Long, User> authorMap = userRepository.findByIds(authorIds).stream()
            .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u));

        List<ArticleDTO> items = new ArrayList<>(pageArticles.size());
        for (int i = 0; i < pageArticles.size(); i++) {
            Article article = pageArticles.get(i);
            User author = authorMap.get(article.getAuthorId().getValue());
            if (author == null) {
                throw new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在");
            }
            ArticleDTO item = articleAssembler.toDTO(article, author);
            item.setRelationSortOrder(visibleSortOrders.get(fromIndex + i));
            items.add(item);
        }
        return new PageResult<>(items, currentPage, currentPageSize, visibleArticles.size());
    }

    /**
     * 获取专栏内某篇文章的上下篇。
     *
     * @param columnId  专栏 ID
     * @param articleId 当前文章 ID
     * @return Map with keys "prev" and "next", values may be null
     */
    public java.util.Map<String, ArticleDTO> getColumnArticleNeighbors(Long columnId, Long articleId) {
        loadPublishedColumn(columnId);
        List<Long> articleIds = columnRepository.findArticleIds(new ColumnId(columnId));

        // Batch-load all articles to filter PUBLISHED ones (eliminates N individual queries)
        Map<Long, Article> articleMap = articleRepository.findByIds(articleIds).stream()
            .collect(Collectors.toMap(a -> a.getId().getValue(), a -> a));

        List<Long> visibleIds = new ArrayList<>();
        for (Long id : articleIds) {
            Article article = articleMap.get(id);
            if (article != null && ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                visibleIds.add(id);
            }
        }

        int idx = visibleIds.indexOf(articleId);
        Long prevId = idx > 0 ? visibleIds.get(idx - 1) : null;
        Long nextId = idx >= 0 && idx < visibleIds.size() - 1 ? visibleIds.get(idx + 1) : null;

        // Batch-load authors for prev and next in one query
        List<Long> neighborIds = new ArrayList<>(2);
        if (prevId != null) neighborIds.add(prevId);
        if (nextId != null) neighborIds.add(nextId);

        Map<Long, User> authorMap = java.util.Collections.emptyMap();
        if (!neighborIds.isEmpty()) {
            List<Long> neighborAuthorIds = neighborIds.stream()
                .map(id -> articleMap.get(id))
                .filter(a -> a != null)
                .map(a -> a.getAuthorId().getValue())
                .distinct()
                .collect(Collectors.toList());
            authorMap = userRepository.findByIds(neighborAuthorIds).stream()
                .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u));
        }

        final Map<Long, User> finalAuthorMap = authorMap;
        java.util.Map<String, ArticleDTO> result = new java.util.LinkedHashMap<>();
        result.put("prev", prevId != null ? toNeighborDTOFromMap(articleMap.get(prevId), finalAuthorMap) : null);
        result.put("next", nextId != null ? toNeighborDTOFromMap(articleMap.get(nextId), finalAuthorMap) : null);
        return result;
    }

    private ArticleDTO toNeighborDTOFromMap(Article article, Map<Long, User> authorMap) {
        if (article == null) return null;
        User author = authorMap.get(article.getAuthorId().getValue());
        return author != null ? articleAssembler.toDTO(article, author) : null;
    }

    /**
     * 订阅专栏。
     *
     * @param columnId 专栏 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void subscribeColumn(Long columnId, Long userId) {
        long _start = System.currentTimeMillis();
        Column column = loadPublishedColumn(columnId);
        ColumnSubscription existing = columnSubscriptionRepository.findByColumnAndUserIncludingDeleted(
            new ColumnId(columnId),
            new UserId(userId)
        ).orElse(null);
        if (existing != null) {
            if (!existing.isDeleted()) {
                throw new ApplicationException(ErrorCode.CONFLICT, "已经订阅过该专栏");
            }
            existing.reactivate();
            columnSubscriptionRepository.save(existing);
        } else {
            columnSubscriptionRepository.save(ColumnSubscription.create(
                columnSubscriptionRepository.nextId(),
                new ColumnId(columnId),
                new UserId(userId)
            ));
        }
        column.increaseSubscriberCount();
        columnRepository.save(column);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "订阅专栏",
            BizLogHelper.params("columnId", columnId),
            BizLogHelper.result("subscribed=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 取消订阅专栏。
     *
     * @param columnId 专栏 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unsubscribeColumn(Long columnId, Long userId) {
        long _start = System.currentTimeMillis();
        Column column = loadPublishedColumn(columnId);
        ColumnSubscription subscription = columnSubscriptionRepository.findByColumnAndUserIncludingDeleted(
            new ColumnId(columnId),
            new UserId(userId)
        ).orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "订阅关系不存在"));
        if (subscription.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消订阅该专栏");
        }
        subscription.delete();
        columnSubscriptionRepository.save(subscription);
        column.decreaseSubscriberCount();
        columnRepository.save(column);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "取消订阅专栏",
            BizLogHelper.params("columnId", columnId),
            BizLogHelper.result("subscribed=false"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 搜索专栏。
     *
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页数量
     * @param currentUserId 当前用户 ID
     * @return 专栏分页结果
     */
    public PageResult<ColumnDTO> searchColumns(String keyword, int page, int pageSize, Long currentUserId) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<Column> columns = columnRepository.searchPublished(keyword, "subscribers", currentPage, currentPageSize);
        List<ColumnDTO> items = new ArrayList<>(columns.size());
        for (Column column : columns) {
            items.add(toDTO(column, currentUserId));
        }
        return new PageResult<>(items, currentPage, currentPageSize, columnRepository.countSearchPublished(keyword));
    }

    private Column loadPublishedColumn(Long columnId) {
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        if (!column.isPublished()) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在");
        }
        return column;
    }

    /**
     * 向专栏添加文章。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param sortOrder 排序值
     */
    @Transactional(rollbackFor = Exception.class)
    public void addColumnArticle(Long columnId, Long articleId, int sortOrder) {
        long _start = System.currentTimeMillis();
        Column column = loadPublishedColumn(columnId);
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "只能将已发布文章添加到专栏");
        }
        columnRepository.bindArticle(column.getId(), articleId, sortOrder);
        homePortalCacheInvalidator.evictBootstrap();
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "专栏添加文章",
            BizLogHelper.trace(),
            BizLogHelper.params("columnId", columnId, "articleId", articleId, "sortOrder", sortOrder),
            BizLogHelper.result("bound=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 从专栏移除文章。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeColumnArticle(Long columnId, Long articleId) {
        long _start = System.currentTimeMillis();
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        columnRepository.unbindArticle(column.getId(), articleId);
        homePortalCacheInvalidator.evictBootstrap();
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "专栏移除文章",
            BizLogHelper.trace(),
            BizLogHelper.params("columnId", columnId, "articleId", articleId),
            BizLogHelper.result("unbound=true"),
            BizLogHelper.elapsed(_start));
    }

    // ==================== 创作者专栏管理方法 ====================

    /**
     * 创作者：查询自己的所有专栏。
     *
     * @param userId 当前用户 ID
     * @return 专栏列表
     */
    public List<ColumnDTO> listMyColumns(Long userId) {
        List<Column> columns = columnRepository.findByAuthorId(userId);
        List<ColumnDTO> result = new ArrayList<>(columns.size());
        for (Column column : columns) {
            ColumnDTO dto = toAdminDTO(column);
            dto.setSubscribed(false);
            result.add(dto);
        }
        return result;
    }

    /**
     * 创作者：创建专栏。
     *
     * @param userId    创作者 ID
     * @param title     专栏标题
     * @param summary   专栏简介
     * @param coverUrl  封面地址
     * @return 创建后的专栏 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnDTO createMyColumn(Long userId, String title, String summary, String coverUrl) {
        long _start = System.currentTimeMillis();
        userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        Column column = Column.create(columnRepository.nextId(), new UserId(userId), title, summary, coverUrl, 0);
        columnRepository.save(column);
        homePortalCacheInvalidator.evictStatsAndBootstrap();
        ColumnDTO result = toAdminDTO(column);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "创建专栏",
            BizLogHelper.params("title", title),
            BizLogHelper.created("columnId", result.getId()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 创作者：更新自己的专栏信息。
     *
     * @param columnId 专栏 ID
     * @param userId   当前用户 ID
     * @param title    新标题
     * @param summary  新简介
     * @param coverUrl 新封面地址
     * @return 更新后的专栏 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnDTO updateMyColumn(Long columnId, Long userId, String title, String summary, String coverUrl) {
        long _start = System.currentTimeMillis();
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        column.update(title, summary, coverUrl, column.getSortOrder(), column.getStatus());
        columnRepository.save(column);
        homePortalCacheInvalidator.evictBootstrap();
        ColumnDTO result = toAdminDTO(column);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "更新专栏",
            BizLogHelper.params("columnId", columnId, "title", title),
            BizLogHelper.created("columnId", result.getId()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 创作者：删除自己的专栏（软删除）。
     *
     * @param columnId 专栏 ID
     * @param userId   当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyColumn(Long columnId, Long userId) {
        long _start = System.currentTimeMillis();
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        boolean published = column.isPublished();
        column.delete();
        columnRepository.save(column);
        if (published) {
            homePortalCacheInvalidator.evictStatsAndBootstrap();
        } else {
            homePortalCacheInvalidator.evictBootstrap();
        }
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "删除专栏",
            BizLogHelper.params("columnId", columnId),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 创作者：向自己的专栏添加文章（文章必须是本人所有）。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param userId    当前用户 ID
     * @param sortOrder 排序值
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMyColumnArticle(Long columnId, Long articleId, Long userId, int sortOrder) {
        long _start = System.currentTimeMillis();
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        com.myblog.domain.model.aggregate.Article article = articleRepository
            .findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!article.getAuthorId().getValue().equals(userId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "只能将自己的文章加入专栏");
        }
        columnRepository.bindArticle(column.getId(), articleId, sortOrder);
        homePortalCacheInvalidator.evictBootstrap();
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "专栏添加文章",
            BizLogHelper.params("columnId", columnId, "articleId", articleId, "sortOrder", sortOrder),
            BizLogHelper.result("bound=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 创作者：从自己的专栏移除文章。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param userId    当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeMyColumnArticle(Long columnId, Long articleId, Long userId) {
        long _start = System.currentTimeMillis();
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        columnRepository.unbindArticle(column.getId(), articleId);
        homePortalCacheInvalidator.evictBootstrap();
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "专栏移除文章",
            BizLogHelper.params("columnId", columnId, "articleId", articleId),
            BizLogHelper.result("unbound=true"),
            BizLogHelper.elapsed(_start));
    }

    private void ensureColumnOwner(Column column, Long userId) {
        if (!column.getAuthorId().getValue().equals(userId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作此专栏");
        }
    }

    // ==================== 管理后台方法 ====================

    /**
     * 后台：分页查询所有专栏（含草稿）。
     */
    public PageResult<ColumnDTO> adminPageColumns(String keyword, String status, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<Column> columns = columnRepository.findAll(keyword, status, currentPage, currentPageSize);
        List<ColumnDTO> items = new ArrayList<>(columns.size());
        for (Column column : columns) {
            items.add(toAdminDTO(column));
        }
        return new PageResult<>(items, currentPage, currentPageSize, columnRepository.countAll(keyword, status));
    }

    /**
     * 后台：创建专栏。
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnDTO adminCreateColumn(Long authorId, String title, String summary,
                                       String coverUrl, Integer sortOrder) {
        long _start = System.currentTimeMillis();
        userRepository.findById(new UserId(authorId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "作者不存在"));
        Column column = Column.create(
            columnRepository.nextId(),
            new UserId(authorId),
            title, summary, coverUrl, sortOrder
        );
        columnRepository.save(column);
        homePortalCacheInvalidator.evictStatsAndBootstrap();
        ColumnDTO result = toAdminDTO(column);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "后台创建专栏",
            BizLogHelper.trace(),
            BizLogHelper.params("title", title, "authorId", authorId),
            BizLogHelper.result("columnId=" + result.getId()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 后台：更新专栏。
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnDTO adminUpdateColumn(Long columnId, String title, String summary,
                                       String coverUrl, Integer sortOrder, String status) {
        long _start = System.currentTimeMillis();
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        boolean wasPublished = column.isPublished();
        column.update(title, summary, coverUrl, sortOrder, status);
        columnRepository.save(column);
        if (wasPublished != column.isPublished()) {
            homePortalCacheInvalidator.evictStatsAndBootstrap();
        } else {
            homePortalCacheInvalidator.evictBootstrap();
        }
        ColumnDTO result = toAdminDTO(column);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "后台更新专栏",
            BizLogHelper.trace(),
            BizLogHelper.params("columnId", columnId, "title", title),
            BizLogHelper.result("columnId=" + columnId),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 后台：删除专栏（软删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteColumn(Long columnId) {
        long _start = System.currentTimeMillis();
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        boolean published = column.isPublished();
        column.delete();
        columnRepository.save(column);
        if (published) {
            homePortalCacheInvalidator.evictStatsAndBootstrap();
        } else {
            homePortalCacheInvalidator.evictBootstrap();
        }
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "后台删除专栏",
            BizLogHelper.trace(),
            BizLogHelper.params("columnId", columnId),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    private ColumnDTO toAdminDTO(Column column) {
        ColumnDTO dto = toBaseDTO(column);
        dto.setStatus(column.getStatus());
        dto.setSortOrder(column.getSortOrder());
        dto.setAuthorId(column.getAuthorId().getValue());
        dto.setCreatedAt(column.getCreatedAt());
        dto.setSubscribed(false);
        userRepository.findById(column.getAuthorId()).ifPresent(u -> dto.setAuthor(UserAssembler.toDTO(u)));
        return dto;
    }

    private ColumnDTO toDTO(Column column, Long currentUserId) {
        User author = userRepository.findById(column.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏作者不存在"));
        ColumnDTO dto = toBaseDTO(column);
        dto.setSubscribed(currentUserId != null && columnSubscriptionRepository.exists(column.getId(), new UserId(currentUserId)));
        dto.setAuthor(UserAssembler.toDTO(author));
        return dto;
    }

    private ColumnDTO toBaseDTO(Column column) {
        ColumnDTO dto = new ColumnDTO();
        dto.setId(column.getId().getValue());
        dto.setTitle(column.getTitle());
        dto.setSummary(column.getSummary());
        dto.setCoverUrl(column.getCoverUrl());
        dto.setSubscriberCount(column.getSubscriberCount());
        dto.setArticleCount(column.getArticleCount());
        dto.setIntro(column.getIntro());
        dto.setDifficulty(column.getDifficulty());
        dto.setEstimatedMinutes(column.getEstimatedMinutes());
        dto.setSourceType(column.getSourceType());
        dto.setSourceNote(column.getSourceNote());
        dto.setRecommended(column.isRecommended());
        dto.setRecommendWeight(column.getRecommendWeight());
        return dto;
    }

    private void populateLearningPath(ColumnDTO dto, Long currentUserId) {
        List<LearningPathArticle> relations = columnRepository.findArticleRelations(new ColumnId(dto.getId()));
        dto.setProgress(learningProgressAppService.buildProgress(
            LearningProgressAppService.ASSET_TYPE_COLUMN,
            dto.getId(),
            relations,
            currentUserId
        ));
        dto.setOutline(learningProgressAppService.buildOutline(
            LearningProgressAppService.ASSET_TYPE_COLUMN,
            dto.getId(),
            relations,
            currentUserId
        ));
        if (dto.getProgress() != null) {
            dto.setNextArticle(dto.getProgress().getNextArticle());
        }
    }
}
