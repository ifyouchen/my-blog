package com.myblog.application.assembler;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

/**
 * 文章应用组装器。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class ArticleAssembler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LEGACY_DEFAULT_COVER_URL = "/api/uploads/files/default/article-cover.png";
    private static final String DEFAULT_COVER_URL = "/api/uploads/files/default/article-cover.svg";

    private final String defaultArticleCoverUrl;

    public ArticleAssembler(
            @Value("${my-blog.default-article-cover-url:}") String defaultArticleCoverUrl) {
        this.defaultArticleCoverUrl = StringUtils.hasText(defaultArticleCoverUrl)
            ? defaultArticleCoverUrl : DEFAULT_COVER_URL;
    }

    /**
     * 将文章领域对象转换为应用 DTO。
     *
     * @param article 文章领域对象
     * @param author 作者领域对象
     * @return 文章应用 DTO
     */
    public ArticleDTO toDTO(Article article, User author) {
        ArticleDTO dto = new ArticleDTO();
        dto.setId(article.getId().getValue());
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setCoverUrl(resolveCoverUrl(article.getCoverUrl()));
        dto.setCategory(article.getCategory());
        dto.setTags(article.getTags());
        dto.setStatus(article.getStatus().name());
        dto.setViewCount(article.getViewCount());
        dto.setLikeCount(article.getLikeCount());
        dto.setFavoriteCount(article.getFavoriteCount());
        dto.setCommentCount(article.getCommentCount());
        dto.setFeatured(article.isFeatured());
        dto.setSlug(article.getSlug());
        dto.setSeoTitle(article.getSeoTitle());
        dto.setSeoDescription(article.getSeoDescription());
        dto.setOfflineReason(article.getOfflineReason());
        dto.setWarnFlag(article.isWarnFlag());
        if (article.getScheduledPublishAt() != null) {
            dto.setScheduledPublishAt(FORMATTER.format(article.getScheduledPublishAt()));
        }
        if (article.getPublishedAt() != null) {
            dto.setPublishedAt(FORMATTER.format(article.getPublishedAt()));
        }
        if (article.getUpdatedAt() != null) {
            dto.setUpdatedAt(FORMATTER.format(article.getUpdatedAt()));
        }
        UserDTO authorDTO = UserAssembler.toDTO(author);
        dto.setAuthor(authorDTO);
        return dto;
    }

    /**
     * 将文章领域对象转换为包含完整内容的应用 DTO（用于详情页）。
     *
     * @param article 文章领域对象
     * @param author 作者领域对象
     * @return 包含完整内容的文章应用 DTO
     */
    public ArticleDTO toDetailDTO(Article article, User author) {
        ArticleDTO dto = toDTO(article, author);
        dto.setContent(article.getContent());
        return dto;
    }

    private String resolveCoverUrl(String coverUrl) {
        if (StringUtils.hasText(coverUrl) && !LEGACY_DEFAULT_COVER_URL.equals(coverUrl)) {
            return coverUrl;
        }
        return defaultArticleCoverUrl;
    }
}
