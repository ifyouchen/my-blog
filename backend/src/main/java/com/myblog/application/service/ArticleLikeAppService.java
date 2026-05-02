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
import com.myblog.shared.util.BizLogHelper;
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

    /**
     * 点赞文章。
     *
     * <p><b>并发安全说明：</b><br>
     * 不再使用「先查后写」模式，改为直接构造已激活状态的点赞对象并调用 {@code save()}。
     * 底层 {@code insertOrUpdate} 的 {@code ON DUPLICATE KEY UPDATE deleted_at = VALUES(deleted_at)}
     * 保证了唯一键约束下的幂等写入。
     * 若并发重复点赞，第一个请求成功，后续请求因查到有效记录而抛出 CONFLICT（快速失败），
     * 不会因唯一键冲突导致 500 错误。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId, Long userId) {
        long _start = System.currentTimeMillis();
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "不能在未发布的文章下点赞");
        }

        // 查询已有记录（包含已软删除的），避免无谓 INSERT 引发唯一键冲突
        Optional<ArticleLike> existing = articleLikeRepository.findAnyByArticleAndUser(
            new ArticleId(articleId), new UserId(userId));

        if (existing.isPresent() && !existing.get().isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经点赞过了");
        }

        // 使用 existing 记录（重新激活）或创建新记录
        ArticleLike likeToSave;
        if (existing.isPresent()) {
            // 重新激活已取消的点赞
            ArticleLike old = existing.get();
            old.reactivate();
            likeToSave = old;
        } else {
            likeToSave = ArticleLike.create(articleLikeRepository.nextId(),
                new ArticleId(articleId), new UserId(userId));
        }

        // insertOrUpdate 保证：新记录 INSERT；已有记录 UPDATE deleted_at=NULL
        articleLikeRepository.save(likeToSave);
        eventPublisher.publishEvent(new ArticleLikedEvent(articleId, userId));
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "点赞文章",
            BizLogHelper.params("articleId", articleId, "title", article.getTitle()),
            BizLogHelper.result("liked=true"),
            BizLogHelper.elapsed(_start));
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId, Long userId) {
        long _start = System.currentTimeMillis();
        articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));

        ArticleLike articleLike = articleLikeRepository.findAnyByArticleAndUser(
            new ArticleId(articleId), new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "点赞记录不存在"));

        if (articleLike.isDeleted()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "已经取消点赞了");
        }

        articleLike.delete();
        articleLikeRepository.save(articleLike);
        eventPublisher.publishEvent(new ArticleUnlikedEvent(articleId, userId));
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "取消文章点赞",
            BizLogHelper.params("articleId", articleId),
            BizLogHelper.result("liked=false"),
            BizLogHelper.elapsed(_start));
    }

    public boolean hasLiked(Long articleId, Long userId) {
        return articleLikeRepository.exists(new ArticleId(articleId), new UserId(userId));
    }
}