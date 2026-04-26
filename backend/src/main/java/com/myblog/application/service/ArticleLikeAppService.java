package com.myblog.application.service;

import com.myblog.application.event.ArticleLikedEvent;
import com.myblog.application.event.ArticleUnlikedEvent;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.ArticleLike;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleLikeRepository;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ArticleLikeAppService {

    private static final Logger log = LoggerFactory.getLogger(ArticleLikeAppService.class);

    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleRepository articleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ArticleLikeAppService(ArticleLikeRepository articleLikeRepository,
                                 ArticleRepository articleRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.articleLikeRepository = articleLikeRepository;
        this.articleRepository = articleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId, Long userId) {
        log.info("User {} liking article {}", userId, articleId);
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        ArticleId targetArticleId = article.getId();
        UserId targetUserId = new UserId(userId);

        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "不能在未发布的文章下点赞");
        }

        Optional<ArticleLike> existingLike = articleLikeRepository.findAnyByArticleAndUser(targetArticleId, targetUserId);

        if (existingLike.isPresent()) {
            ArticleLike like = existingLike.get();
            reactivateLike(like);
            articleLikeRepository.save(like);
            eventPublisher.publishEvent(new ArticleLikedEvent(articleId, userId));
            log.info("User {} re-liked article {}", userId, articleId);
        } else {
            ArticleLike newLike = ArticleLike.create(articleLikeRepository.nextId(), targetArticleId, targetUserId);
            articleLikeRepository.save(newLike);
            eventPublisher.publishEvent(new ArticleLikedEvent(articleId, userId));
            log.info("User {} liked article {} (new like)", userId, articleId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId, Long userId) {
        log.info("User {} un-liking article {}", userId, articleId);
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleLike articleLike = articleLikeRepository.findAnyByArticleAndUser(new ArticleId(articleId), new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "点赞记录不存在"));

        if (articleLike.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消点赞了");
        }

        articleLike.delete();
        articleLikeRepository.save(articleLike);
        eventPublisher.publishEvent(new ArticleUnlikedEvent(articleId, userId));
        log.info("User {} un-liked article {}", userId, articleId);
    }

    public boolean hasLiked(Long articleId, Long userId) {
        return articleLikeRepository.exists(new ArticleId(articleId), new UserId(userId));
    }

    private ArticleLike reactivateLike(ArticleLike articleLike) {
        if (!articleLike.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经点赞过了");
        }
        articleLike.reactivate();
        return articleLike;
    }
}