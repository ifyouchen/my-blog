package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.dto.ArticleDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文章收藏应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ArticleFavoriteAppService {

    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public ArticleFavoriteAppService(ArticleFavoriteRepository articleFavoriteRepository,
                                     ArticleRepository articleRepository,
                                     UserRepository userRepository) {
        this.articleFavoriteRepository = articleFavoriteRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    /**
     * 收藏文章。
     *
     * @param articleId 文章 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void favoriteArticle(Long articleId, Long userId) {
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
            article.increaseFavoriteCount();
            articleRepository.save(article);
            return;
        }

        ArticleFavorite favorite = ArticleFavorite.create(
            articleFavoriteRepository.nextId(),
            article.getId(),
            new UserId(userId)
        );
        articleFavoriteRepository.save(favorite);

        article.increaseFavoriteCount();
        articleRepository.save(article);
    }

    /**
     * 取消收藏文章。
     *
     * @param articleId 文章 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleFavorite favorite = articleFavoriteRepository.findByArticleAndUser(new ArticleId(articleId), new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "收藏记录不存在"));

        if (favorite.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消收藏了");
        }

        favorite.delete();
        articleFavoriteRepository.save(favorite);

        article.decreaseFavoriteCount();
        articleRepository.save(article);
    }

    /**
     * 查询是否已收藏。
     *
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 是否已收藏
     */
    public boolean hasFavorited(Long articleId, Long userId) {
        return articleFavoriteRepository.exists(new ArticleId(articleId), new UserId(userId));
    }

    /**
     * 获取我的收藏列表。
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 收藏的文章列表
     */
    public PageResult<ArticleDTO> getUserFavorites(Long userId, int page, int pageSize) {
        List<ArticleFavorite> favorites = articleFavoriteRepository.findByUserId(new UserId(userId), page, pageSize);
        List<ArticleDTO> result = new ArrayList<>();

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
            result.add(ArticleAssembler.toDTO(article, userOpt.get()));
        }
        return new PageResult<ArticleDTO>(
            result,
            page,
            pageSize,
            articleFavoriteRepository.countByUserId(new UserId(userId))
        );
    }
}
