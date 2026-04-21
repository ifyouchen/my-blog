package com.myblog.application.service;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.ArticleLike;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleLikeRepository;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章点赞应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ArticleLikeAppService {

    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleRepository articleRepository;

    public ArticleLikeAppService(ArticleLikeRepository articleLikeRepository, ArticleRepository articleRepository) {
        this.articleLikeRepository = articleLikeRepository;
        this.articleRepository = articleRepository;
    }

    /**
     * 点赞文章。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ArticleId targetArticleId = article.getId();
        UserId targetUserId = new UserId(userId);

        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "不能在未发布的文章下点赞");
        }

        ArticleLike articleLike = articleLikeRepository.findAnyByArticleAndUser(targetArticleId, targetUserId)
            .map(existingLike -> reactivateLike(existingLike))
            .orElseGet(() -> ArticleLike.create(articleLikeRepository.nextId(), targetArticleId, targetUserId));

        articleLikeRepository.save(articleLike);

        article.increaseLikeCount();
        articleRepository.save(article);
    }

    /**
     * 取消点赞文章。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleLike articleLike = articleLikeRepository.findAnyByArticleAndUser(new ArticleId(articleId), new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "点赞记录不存在"));

        if (articleLike.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消点赞了");
        }

        articleLike.delete();
        articleLikeRepository.save(articleLike);

        article.decreaseLikeCount();
        articleRepository.save(article);
    }

    /**
     * 查询用户是否已点赞文章。
     *
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    public boolean hasLiked(Long articleId, Long userId) {
        return articleLikeRepository.exists(new ArticleId(articleId), new UserId(userId));
    }

    /**
     * 恢复已取消的点赞记录。
     *
     * @param articleLike 点赞聚合
     * @return 恢复后的点赞聚合
     */
    private ArticleLike reactivateLike(ArticleLike articleLike) {
        if (!articleLike.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经点赞过了");
        }
        articleLike.reactivate();
        return articleLike;
    }
}
