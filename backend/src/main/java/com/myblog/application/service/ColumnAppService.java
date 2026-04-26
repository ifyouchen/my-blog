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
