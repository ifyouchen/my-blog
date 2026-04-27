package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.command.CreateArticleCommand;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.event.ArticleViewedEvent;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleFavoriteRepository;
import com.myblog.domain.repository.ArticleLikeRepository;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ArticleAppService {

    private static final String LEGACY_DEFAULT_COVER_URL = "/api/uploads/files/default/article-cover.png";
    private static final String DEFAULT_COVER_URL = "/api/uploads/files/default/article-cover.svg";

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final ArticleAssembler articleAssembler;
    private final ApplicationEventPublisher eventPublisher;
    private final String defaultArticleCoverUrl;

    public ArticleAppService(ArticleRepository articleRepository,
                             ArticleLikeRepository articleLikeRepository,
                             ArticleFavoriteRepository articleFavoriteRepository,
                             UserFollowRepository userFollowRepository,
                             UserRepository userRepository,
                             ArticleAssembler articleAssembler,
                             ApplicationEventPublisher eventPublisher,
                             @Value("${my-blog.default-article-cover-url:}") String defaultArticleCoverUrl) {
        this.articleRepository = articleRepository;
        this.articleLikeRepository = articleLikeRepository;
        this.articleFavoriteRepository = articleFavoriteRepository;
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.articleAssembler = articleAssembler;
        this.eventPublisher = eventPublisher;
        this.defaultArticleCoverUrl = StringUtils.hasText(defaultArticleCoverUrl)
            ? defaultArticleCoverUrl : DEFAULT_COVER_URL;
    }

    /**
     * 分页查询已发布文章。
     *
     * @param query 文章分页查询参数
     * @return 文章分页结果
     */
    public PageResult<ArticleDTO> pagePublishedArticles(ArticlePageQuery query) {
        int page = Math.max(query.getPage(), 1);
        int pageSize = Math.max(query.getPageSize(), 1);
        int offset = (page - 1) * pageSize;

        // Check if enhanced search is needed
        boolean needsEnhancedSearch = query.getAuthorKeyword() != null && !query.getAuthorKeyword().isEmpty()
            || query.getDateFrom() != null && !query.getDateFrom().isEmpty()
            || query.getDateTo() != null && !query.getDateTo().isEmpty();

        List<Article> articles;
        long total;

        if (query.isFollowingOnly() && query.getCurrentUserId() != null) {
            // Get following authors
            List<Long> followingIds = getFollowingAuthorIds(query.getCurrentUserId());
            if (followingIds.isEmpty()) {
                return new PageResult<>(new ArrayList<>(), page, pageSize, 0);
            }
            if (needsEnhancedSearch) {
                articles = articleRepository.findPublishedEnhancedByAuthorIds(
                    followingIds,
                    query.getKeyword(),
                    query.getCategory(),
                    query.getTag(),
                    query.getSort(),
                    query.getAuthorKeyword(),
                    query.getDateFrom(),
                    query.getDateTo(),
                    page,
                    pageSize
                );
                total = articleRepository.countPublishedEnhancedByAuthorIds(
                    followingIds,
                    query.getKeyword(),
                    query.getCategory(),
                    query.getTag(),
                    query.getAuthorKeyword(),
                    query.getDateFrom(),
                    query.getDateTo()
                );
            } else {
                articles = articleRepository.findPublishedByAuthorIds(
                    followingIds,
                    query.getSort(),
                    page,
                    pageSize
                );
                total = articleRepository.countPublishedByAuthorIds(followingIds);
            }
        } else if (needsEnhancedSearch) {
            articles = articleRepository.findPublishedEnhanced(
                query.getKeyword(),
                query.getCategory(),
                query.getTag(),
                query.getSort(),
                query.getAuthorKeyword(),
                query.getDateFrom(),
                query.getDateTo(),
                pageSize,
                offset
            );
            total = articleRepository.countPublishedEnhanced(
                query.getKeyword(),
                query.getCategory(),
                query.getTag(),
                query.getAuthorKeyword(),
                query.getDateFrom(),
                query.getDateTo()
            );
        } else {
            articles = articleRepository.findPublishedWithLimit(
                query.getKeyword(),
                query.getCategory(),
                query.getTag(),
                query.getSort(),
                pageSize,
                offset
            );
            total = articleRepository.countPublished(
                query.getKeyword(),
                query.getCategory(),
                query.getTag()
            );
        }

        List<ArticleDTO> items = toDTOList(articles);
        return new PageResult<ArticleDTO>(items, page, pageSize, total);
    }

    private List<ArticleDTO> toDTOList(List<Article> articles) {
        if (articles.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查询作者，解决 N+1 问题
        List<Long> authorIds = articles.stream()
            .map(a -> a.getAuthorId().getValue())
            .distinct()
            .collect(Collectors.toList());
        Map<Long, User> authorMap = userRepository.findByIds(authorIds).stream()
            .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u));

        List<ArticleDTO> items = new ArrayList<>(articles.size());
        for (Article article : articles) {
            User author = authorMap.get(article.getAuthorId().getValue());
            if (author != null) {
                items.add(buildDto(article, author, null));
            }
        }
        return items;
    }

    private List<Long> getFollowingAuthorIds(Long userId) {
        return userFollowRepository.findFollowingUserIds(new UserId(userId));
    }

    /**
     * 获取文章详情并增加阅读量。
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @Transactional(readOnly = true)
    public ArticleDTO getArticleDetail(Long articleId, Long currentUserId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!canAccessArticle(article, currentUserId, currentUserRole)) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            eventPublisher.publishEvent(new ArticleViewedEvent(articleId));
        }
        return buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), currentUserId);
    }

    /**
     * 创建文章。
     *
     * @param command 创建文章命令
     * @return 创建后的文章
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO createArticle(CreateArticleCommand command) {
        User author = userRepository.findById(new UserId(command.getAuthorId()))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        Article article = Article.create(
            articleRepository.nextId(),
            author.getId(),
            command.getTitle(),
            command.getSummary(),
            command.getContent(),
            resolveCoverUrl(command.getCoverUrl()),
            command.getCategory(),
            command.getTags(),
            resolveCreateStatus(command.getStatus())
        );
        articleRepository.save(article);
        return buildDetailDto(article, author, command.getAuthorId());
    }

    /**
     * 获取编辑态文章。
     *
     * @param articleId 文章 ID
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 文章详情
     */
    public ArticleDTO getArticleForEdit(Long articleId, Long userId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        return buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), userId);
    }

    /**
     * 更新文章。
     *
     * @param articleId 文章 ID
     * @param command 更新命令
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 更新后的文章
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO updateArticle(Long articleId, CreateArticleCommand command, Long userId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        article.updateContent(
            command.getTitle(),
            command.getSummary(),
            command.getContent(),
            resolveCoverUrl(command.getCoverUrl()),
            command.getCategory(),
            command.getTags()
        );
        applyStatus(article, command.getStatus());
        articleRepository.save(article);
        return buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), userId);
    }

    /**
     * 更新文章状态。
     *
     * @param articleId 文章 ID
     * @param status 目标状态
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     * @return 更新后的文章
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO updateArticleStatus(Long articleId, String status, Long userId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        applyStatus(article, status);
        articleRepository.save(article);
        return buildDetailDto(article, userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在")), userId);
    }

    /**
     * 删除文章。
     *
     * @param articleId 文章 ID
     * @param userId 当前用户 ID
     * @param currentUserRole 当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long articleId, Long userId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ensureCanManage(article, userId, currentUserRole);
        article.delete();
        articleRepository.save(article);
    }

    private ArticleDTO toDTO(Article article, Long currentUserId) {
        User author = userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
        return buildDto(article, author, currentUserId);
    }

    private PageResult<ArticleDTO> buildPageResult(List<Article> articles, int page, int pageSize) {
        int fromIndex = Math.min((page - 1) * pageSize, articles.size());
        int toIndex = Math.min(fromIndex + pageSize, articles.size());
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        for (Article article : articles.subList(fromIndex, toIndex)) {
            items.add(toDTO(article, null));
        }
        return new PageResult<ArticleDTO>(items, page, pageSize, articles.size());
    }

    private boolean canAccessArticle(Article article, Long currentUserId, String currentUserRole) {
        if (ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            return true;
        }
        if (currentUserId == null) {
            return false;
        }
        return article.getAuthorId().getValue().equals(currentUserId)
            || UserRole.ADMIN.name().equals(currentUserRole);
    }

    private void ensureCanManage(Article article, Long userId, String currentUserRole) {
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (article.getAuthorId().getValue().equals(userId) || UserRole.ADMIN.name().equals(currentUserRole)) {
            return;
        }
        throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作这篇文章");
    }

    private ArticleStatus resolveCreateStatus(String status) {
        if (ArticleStatus.PUBLISHED.name().equals(status)) {
            return ArticleStatus.PUBLISHED;
        }
        if (ArticleStatus.OFFLINE.name().equals(status)) {
            return ArticleStatus.OFFLINE;
        }
        return ArticleStatus.DRAFT;
    }

    private void applyStatus(Article article, String status) {
        if (!StringUtils.hasText(status)) {
            article.saveDraft();
            return;
        }
        if (ArticleStatus.PUBLISHED.name().equals(status)) {
            article.publish();
            return;
        }
        if (ArticleStatus.OFFLINE.name().equals(status)) {
            article.offline();
            return;
        }
        if (!ArticleStatus.DRAFT.name().equals(status)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不支持的文章状态");
        }
        article.saveDraft();
    }

    private String resolveCoverUrl(String coverUrl) {
        if (StringUtils.hasText(coverUrl) && !LEGACY_DEFAULT_COVER_URL.equals(coverUrl)) {
            return coverUrl.trim();
        }
        return defaultArticleCoverUrl;
    }

    private ArticleDTO buildDto(Article article, User author, Long currentUserId) {
        ArticleDTO dto = articleAssembler.toDTO(article, author);
        populateUserStatus(dto, article, currentUserId);
        return dto;
    }

    private ArticleDTO buildDetailDto(Article article, User author, Long currentUserId) {
        ArticleDTO dto = articleAssembler.toDetailDTO(article, author);
        populateUserStatus(dto, article, currentUserId);
        return dto;
    }

    private void populateUserStatus(ArticleDTO dto, Article article, Long currentUserId) {
        if (dto.getAuthor() != null) {
            dto.getAuthor().setFollowed(resolveAuthorFollowed(article, currentUserId));
        }
        if (currentUserId == null) {
            dto.setLiked(false);
            dto.setFavorited(false);
            return;
        }
        UserId currentUser = new UserId(currentUserId);
        dto.setLiked(articleLikeRepository.exists(article.getId(), currentUser));
        dto.setFavorited(articleFavoriteRepository.exists(article.getId(), currentUser));
    }

    private boolean resolveAuthorFollowed(Article article, Long currentUserId) {
        if (currentUserId == null || article.getAuthorId().getValue().equals(currentUserId)) {
            return false;
        }
        return userFollowRepository.exists(new UserId(currentUserId), article.getAuthorId());
    }
}
