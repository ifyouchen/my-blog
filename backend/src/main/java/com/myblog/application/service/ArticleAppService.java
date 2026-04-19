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
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;

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
        List<Article> articles = articleRepository.findPublished(query.getKeyword(), query.getCategory(), query.getTag());
        int fromIndex = Math.min((query.getPage() - 1) * query.getPageSize(), articles.size());
        int toIndex = Math.min(fromIndex + query.getPageSize(), articles.size());
        List<ArticleDTO> items = new ArrayList<ArticleDTO>();
        for (Article article : articles.subList(fromIndex, toIndex)) {
            items.add(toDTO(article));
        }
        return new PageResult<ArticleDTO>(items, query.getPage(), query.getPageSize(), articles.size());
    }

    /**
     * 获取文章详情并增加阅读量。
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    public ArticleDTO getArticleDetail(Long articleId) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        article.increaseViewCount();
        articleRepository.save(article);
        return toDTO(article);
    }

    /**
     * 创建文章。
     *
     * @param command 创建文章命令
     * @return 创建后的文章
     */
    public ArticleDTO createArticle(CreateArticleCommand command) {
        User author = userRepository.findById(new UserId(command.getAuthorId()))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        ArticleStatus status = ArticleStatus.DRAFT;
        if ("PUBLISHED".equals(command.getStatus())) {
            status = ArticleStatus.PUBLISHED;
        }
        Article article = Article.create(
            articleRepository.nextId(),
            author.getId(),
            command.getTitle(),
            command.getSummary(),
            command.getContent(),
            command.getCoverUrl(),
            command.getCategory(),
            command.getTags(),
            status
        );
        articleRepository.save(article);
        return ArticleAssembler.toDTO(article, author);
    }

    private ArticleDTO toDTO(Article article) {
        User author = userRepository.findById(article.getAuthorId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章作者不存在"));
        return ArticleAssembler.toDTO(article, author);
    }
}
