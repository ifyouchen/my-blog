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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Transactional(rollbackFor = Exception.class)
    public void favoriteArticle(Long articleId, Long userId) {
        log.info("User {} favoriting article {}", userId, articleId);
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "不能在未发布的文章下收藏");
        }

        Optional<ArticleFavorite> existingFavorite = articleFavoriteRepository.findByArticleAndUserIncludingDeleted(
            new ArticleId(articleId), new UserId(userId)
        );
        if (existingFavorite.isPresent()) {
            ArticleFavorite favorite = existingFavorite.get();
            if (!favorite.isDeleted()) {
                throw new ApplicationException(ErrorCode.CONFLICT, "已经收藏过了");
            }
            favorite.reactivate();
            articleFavoriteRepository.save(favorite);
            eventPublisher.publishEvent(new ArticleFavoritedEvent(articleId, userId));
            log.info("User {} re-favorited article {}", userId, articleId);
            return;
        }

        ArticleFavorite favorite = ArticleFavorite.create(
            articleFavoriteRepository.nextId(),
            article.getId(),
            new UserId(userId)
        );
        articleFavoriteRepository.save(favorite);
        eventPublisher.publishEvent(new ArticleFavoritedEvent(articleId, userId));
        log.info("User {} favorited article {} (new)", userId, articleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteArticle(Long articleId, Long userId) {
        log.info("User {} unfavoriting article {}", userId, articleId);
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleFavorite favorite = articleFavoriteRepository.findByArticleAndUser(new ArticleId(articleId), new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "收藏记录不存在"));

        if (favorite.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消收藏了");
        }

        favorite.delete();
        articleFavoriteRepository.save(favorite);
        eventPublisher.publishEvent(new ArticleUnfavoritedEvent(articleId, userId));
        log.info("User {} unfavorited article {}", userId, articleId);
    }

    public boolean hasFavorited(Long articleId, Long userId) {
        return articleFavoriteRepository.exists(new ArticleId(articleId), new UserId(userId));
    }

    public PageResult<ArticleDTO> getUserFavorites(Long userId, int page, int pageSize) {
        UserId currentUserId = new UserId(userId);
        int rawTotal = articleFavoriteRepository.countByUserId(currentUserId);
        if (rawTotal <= 0) {
            return new PageResult<ArticleDTO>(new ArrayList<ArticleDTO>(), page, pageSize, 0);
        }

        List<ArticleFavorite> favorites = articleFavoriteRepository.findByUserId(currentUserId, 1, rawTotal);
        List<ArticleDTO> visibleFavorites = new ArrayList<ArticleDTO>();

        for (ArticleFavorite favorite : favorites) {
            Optional<Article> articleOpt = articleRepository.findById(favorite.getArticleId());
            if (!articleOpt.isPresent()) {
                continue;
            }
            Article article = articleOpt.get();
            if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                continue;
            }
            Optional<User> userOpt = userRepository.findById(article.getAuthorId());
            if (!userOpt.isPresent()) {
                continue;
            }
            ArticleDTO articleDTO = articleAssembler.toDTO(article, userOpt.get());
            if (favorite.getCreatedAt() != null) {
                articleDTO.setFavoritedAt(FORMATTER.format(favorite.getCreatedAt()));
            }
            visibleFavorites.add(articleDTO);
        }

        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, visibleFavorites.size());
        int toIndex = Math.min(fromIndex + safePageSize, visibleFavorites.size());
        List<ArticleDTO> result = new ArrayList<ArticleDTO>(visibleFavorites.subList(fromIndex, toIndex));
        return new PageResult<ArticleDTO>(
            result,
            safePage,
            safePageSize,
            visibleFavorites.size()
        );
    }
}