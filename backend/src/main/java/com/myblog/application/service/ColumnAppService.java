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
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.ColumnSubscriptionRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 专栏应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ColumnAppService {

    private final ColumnRepository columnRepository;
    private final ColumnSubscriptionRepository columnSubscriptionRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ArticleAssembler articleAssembler;

    public ColumnAppService(ColumnRepository columnRepository,
                            ColumnSubscriptionRepository columnSubscriptionRepository,
                            UserRepository userRepository,
                            ArticleRepository articleRepository,
                            ArticleAssembler articleAssembler) {
        this.columnRepository = columnRepository;
        this.columnSubscriptionRepository = columnSubscriptionRepository;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.articleAssembler = articleAssembler;
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
        return toDTO(column, currentUserId);
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
        List<Long> articleIds = columnRepository.findArticleIds(new ColumnId(columnId));
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        List<Article> visibleArticles = new ArrayList<Article>();
        for (Long articleId : articleIds) {
            Article article = articleRepository.findById(new ArticleId(articleId)).orElse(null);
            if (article != null && ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                visibleArticles.add(article);
            }
        }
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, visibleArticles.size());
        int toIndex = Math.min(fromIndex + currentPageSize, visibleArticles.size());
        for (Article article : visibleArticles.subList(fromIndex, toIndex)) {
            User author = userRepository.findById(article.getAuthorId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
            items.add(articleAssembler.toDTO(article, author));
        }
        return new PageResult<ArticleDTO>(items, currentPage, currentPageSize, visibleArticles.size());
    }

    /**
     * 订阅专栏。
     *
     * @param columnId 专栏 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void subscribeColumn(Long columnId, Long userId) {
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
    }

    /**
     * 取消订阅专栏。
     *
     * @param columnId 专栏 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unsubscribeColumn(Long columnId, Long userId) {
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
        Column column = loadPublishedColumn(columnId);
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "只能将已发布文章添加到专栏");
        }
        columnRepository.bindArticle(column.getId(), articleId, sortOrder);
    }

    /**
     * 从专栏移除文章。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeColumnArticle(Long columnId, Long articleId) {
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        columnRepository.unbindArticle(column.getId(), articleId);
    }

    // ==================== 创作者专栏管理方法 ====================

    /** 最多可创建的专栏数 */
    private static final int MAX_COLUMNS_PER_USER = 5;

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
     * 创作者：创建专栏（每用户最多 5 个）。
     *
     * @param userId    创作者 ID
     * @param title     专栏标题
     * @param summary   专栏简介
     * @param coverUrl  封面地址
     * @return 创建后的专栏 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnDTO createMyColumn(Long userId, String title, String summary, String coverUrl) {
        userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        int existing = columnRepository.countByAuthorId(userId);
        if (existing >= MAX_COLUMNS_PER_USER) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR,
                "每个用户最多创建 " + MAX_COLUMNS_PER_USER + " 个专栏");
        }
        Column column = Column.create(columnRepository.nextId(), new UserId(userId), title, summary, coverUrl, 0);
        columnRepository.save(column);
        return toAdminDTO(column);
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
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        column.update(title, summary, coverUrl, column.getSortOrder(), column.getStatus());
        columnRepository.save(column);
        return toAdminDTO(column);
    }

    /**
     * 创作者：删除自己的专栏（软删除）。
     *
     * @param columnId 专栏 ID
     * @param userId   当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyColumn(Long columnId, Long userId) {
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        column.delete();
        columnRepository.save(column);
    }

    /**
     * 创作者：向自己的专栏添加文章（文章必须是本人所有）。
     *
     * @param columnId  专栏 ID
     * @param articleId 文章 ID
     * @param userId    当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMyColumnArticle(Long columnId, Long articleId, Long userId) {
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        com.myblog.domain.model.aggregate.Article article = articleRepository
            .findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!article.getAuthorId().getValue().equals(userId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "只能将自己的文章加入专栏");
        }
        columnRepository.bindArticle(column.getId(), articleId, 0);
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
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        ensureColumnOwner(column, userId);
        columnRepository.unbindArticle(column.getId(), articleId);
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
    public PageResult<ColumnDTO> adminPageColumns(String keyword, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        List<Column> columns = columnRepository.findAll(keyword, currentPage, currentPageSize);
        List<ColumnDTO> items = new ArrayList<>(columns.size());
        for (Column column : columns) {
            items.add(toAdminDTO(column));
        }
        return new PageResult<>(items, currentPage, currentPageSize, columnRepository.countAll(keyword));
    }

    /**
     * 后台：创建专栏。
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnDTO adminCreateColumn(Long authorId, String title, String summary,
                                       String coverUrl, Integer sortOrder) {
        userRepository.findById(new UserId(authorId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "作者不存在"));
        Column column = Column.create(
            columnRepository.nextId(),
            new UserId(authorId),
            title, summary, coverUrl, sortOrder
        );
        columnRepository.save(column);
        return toAdminDTO(column);
    }

    /**
     * 后台：更新专栏。
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnDTO adminUpdateColumn(Long columnId, String title, String summary,
                                       String coverUrl, Integer sortOrder, String status) {
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        column.update(title, summary, coverUrl, sortOrder, status);
        columnRepository.save(column);
        return toAdminDTO(column);
    }

    /**
     * 后台：删除专栏（软删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteColumn(Long columnId) {
        Column column = columnRepository.findById(new ColumnId(columnId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "专栏不存在"));
        column.delete();
        columnRepository.save(column);
    }

    private ColumnDTO toAdminDTO(Column column) {
        ColumnDTO dto = new ColumnDTO();
        dto.setId(column.getId().getValue());
        dto.setTitle(column.getTitle());
        dto.setSummary(column.getSummary());
        dto.setCoverUrl(column.getCoverUrl());
        dto.setSubscriberCount(column.getSubscriberCount());
        dto.setArticleCount(column.getArticleCount());
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
        ColumnDTO dto = new ColumnDTO();
        dto.setId(column.getId().getValue());
        dto.setTitle(column.getTitle());
        dto.setSummary(column.getSummary());
        dto.setCoverUrl(column.getCoverUrl());
        dto.setSubscriberCount(column.getSubscriberCount());
        dto.setArticleCount(column.getArticleCount());
        dto.setSubscribed(currentUserId != null && columnSubscriptionRepository.exists(column.getId(), new UserId(currentUserId)));
        dto.setAuthor(UserAssembler.toDTO(author));
        return dto;
    }
}
