package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.event.ArticleFavoritedEvent;
import com.myblog.application.event.ArticleUnfavoritedEvent;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.ArticleFavorite;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleFavoriteRepository;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ArticleFavoriteAppService {

    private static final Logger log = LoggerFactory.getLogger(ArticleFavoriteAppService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ArticleAssembler articleAssembler;
    private final ApplicationEventPublisher eventPublisher;

    public ArticleFavoriteAppService(ArticleFavoriteRepository articleFavoriteRepository,
                                     ArticleRepository articleRepository,
                                     UserRepository userRepository,
                                     ArticleAssembler articleAssembler,
                                     ApplicationEventPublisher eventPublisher) {
        this.articleFavoriteRepository = articleFavoriteRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.articleAssembler = articleAssembler;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 收藏文章。
     *
     * <p><b>并发安全说明：</b><br>
     * 先查询包含已软删除的记录，若当前有效收藏存在则快速返回 CONFLICT；
     * 否则构造激活状态的收藏对象并 save()，底层 {@code insertOrUpdate} 的
     * {@code ON DUPLICATE KEY UPDATE deleted_at = VALUES(deleted_at)}
     * 保证唯一键约束下的幂等写入，彻底消除并发重复收藏时的唯一键冲突。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void favoriteArticle(Long articleId, Long userId) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "不能在未发布的文章下收藏");
        }

        Optional<ArticleFavorite> existing = articleFavoriteRepository.findByArticleAndUserIncludingDeleted(
            new ArticleId(articleId), new UserId(userId)
        );

        if (existing.isPresent() && !existing.get().isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经收藏过了");
        }

        ArticleFavorite favoriteToSave;
        if (existing.isPresent()) {
            // 重新激活已取消的收藏
            ArticleFavorite old = existing.get();
            old.reactivate();
            favoriteToSave = old;
        } else {
            favoriteToSave = ArticleFavorite.create(
                articleFavoriteRepository.nextId(),
                article.getId(),
                new UserId(userId)
            );
        }

        articleFavoriteRepository.save(favoriteToSave);
        eventPublisher.publishEvent(new ArticleFavoritedEvent(articleId, userId));
        log.info("{} | {} 收藏文章 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            BizLogHelper.params("articleId", articleId, "title", article.getTitle()),
            BizLogHelper.result("favorited=true"),
            BizLogHelper.elapsed(_start));
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteArticle(Long articleId, Long userId) {
        long _start = System.currentTimeMillis();
        articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleFavorite favorite = articleFavoriteRepository.findByArticleAndUser(
            new ArticleId(articleId), new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "收藏记录不存在"));

        if (favorite.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消收藏了");
        }

        favorite.delete();
        articleFavoriteRepository.save(favorite);
        eventPublisher.publishEvent(new ArticleUnfavoritedEvent(articleId, userId));
        log.info("{} | {} 取消收藏文章 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            BizLogHelper.params("articleId", articleId),
            BizLogHelper.result("favorited=false"),
            BizLogHelper.elapsed(_start));
    }

    public boolean hasFavorited(Long articleId, Long userId) {
        return articleFavoriteRepository.exists(new ArticleId(articleId), new UserId(userId));
    }

    public PageResult<ArticleDTO> getUserFavorites(Long userId, int page, int pageSize) {
        return getUserFavorites(userId, page, pageSize, null);
    }

    public PageResult<ArticleDTO> getUserFavorites(Long userId, int page, int pageSize, String keyword) {
        UserId currentUserId = new UserId(userId);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String trimmedKeyword = hasKeyword ? keyword.trim() : null;

        int total = hasKeyword
            ? articleFavoriteRepository.countPublishedByUserIdAndKeyword(currentUserId, trimmedKeyword)
            : articleFavoriteRepository.countPublishedByUserId(currentUserId);
        if (total <= 0) {
            return new PageResult<ArticleDTO>(new ArrayList<ArticleDTO>(), safePage, safePageSize, 0);
        }

        List<ArticleFavorite> favorites = hasKeyword
            ? articleFavoriteRepository.findPublishedByUserIdAndKeyword(currentUserId, trimmedKeyword, safePage, safePageSize)
            : articleFavoriteRepository.findPublishedByUserId(currentUserId, safePage, safePageSize);
        if (favorites.isEmpty()) {
            return new PageResult<ArticleDTO>(new ArrayList<ArticleDTO>(), safePage, safePageSize, total);
        }

        List<Long> articleIds = new ArrayList<Long>(favorites.size());
        for (ArticleFavorite favorite : favorites) {
            articleIds.add(favorite.getArticleId().getValue());
        }

        Map<Long, Article> articleMap = new HashMap<Long, Article>();
        Set<Long> authorIds = new HashSet<Long>();
        for (Article article : articleRepository.findByIds(articleIds)) {
            articleMap.put(article.getId().getValue(), article);
            authorIds.add(article.getAuthorId().getValue());
        }

        Map<Long, User> authorMap = new HashMap<Long, User>();
        for (User author : userRepository.findByIds(new ArrayList<Long>(authorIds))) {
            authorMap.put(author.getId().getValue(), author);
        }

        List<ArticleDTO> result = new ArrayList<ArticleDTO>(favorites.size());
        for (ArticleFavorite favorite : favorites) {
            Article article = articleMap.get(favorite.getArticleId().getValue());
            if (article == null || !ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                continue;
            }
            User author = authorMap.get(article.getAuthorId().getValue());
            if (author == null) {
                continue;
            }
            ArticleDTO articleDTO = articleAssembler.toDTO(article, author);
            articleDTO.setFavorited(true);
            if (favorite.getCreatedAt() != null) {
                articleDTO.setFavoritedAt(FORMATTER.format(favorite.getCreatedAt()));
            }
            result.add(articleDTO);
        }

        return new PageResult<ArticleDTO>(result, safePage, safePageSize, total);
    }
}
