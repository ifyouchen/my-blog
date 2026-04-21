package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.command.CreateArticleCommand;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ArticleAppService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    /**
     * 创建文章应用服务。
     *
     * @param articleRepository 文章仓储
     * @param userRepository 用户仓储
     */
    public ArticleAppService(ArticleRepository articleRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    /**
     * 分页查询已发布文章。
     *
     * @param query 文章分页查询参数
     * @return 文章分页结果
     */
    public PageResult<ArticleDTO> pagePublishedArticles(ArticlePageQuery query) {
        List<Article> articles = articleRepository.findPublished(
            query.getKeyword(),
            query.getCategory(),
            query.getTag(),
            query.getSort()
        );
        return buildPageResult(articles, query.getPage(), query.getPageSize());
    }

    /**
     * 获取文章详情并增加阅读量。
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    public ArticleDTO getArticleDetail(Long articleId, Long currentUserId, String currentUserRole) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!canAccessArticle(article, currentUserId, currentUserRole)) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            article.increaseViewCount();
            articleRepository.save(article);
        }
        return toDTO(article);
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
            command.getCoverUrl(),
            command.getCategory(),
            command.getTags(),
            resolveCreateStatus(command.getStatus())
        );
        articleRepository.save(article);
        return ArticleAssembler.toDTO(article, author);
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
        return toDTO(article);
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
            command.getCoverUrl(),
            command.getCategory(),
            command.getTags()
        );
        applyStatus(article, command.getStatus());
        articleRepository.save(article);
        return toDTO(article);
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

    private ArticleDTO toDTO(Article article) {
        User author = userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
        return ArticleAssembler.toDTO(article, author);
    }

    private PageResult<ArticleDTO> buildPageResult(List<Article> articles, int page, int pageSize) {
        int fromIndex = Math.min((page - 1) * pageSize, articles.size());
        int toIndex = Math.min(fromIndex + pageSize, articles.size());
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        for (Article article : articles.subList(fromIndex, toIndex)) {
            items.add(toDTO(article));
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
        if (ArticleStatus.PUBLISHED.name().equals(status)) {
            article.publish();
            return;
        }
        if (ArticleStatus.OFFLINE.name().equals(status)) {
            article.offline();
            return;
        }
        article.saveDraft();
    }
}
