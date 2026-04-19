package com.myblog.application.assembler;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;

import java.time.format.DateTimeFormatter;

/**
 * 文章应用组装器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ArticleAssembler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ArticleAssembler() {
    }

    /**
     * 将文章领域对象转换为应用 DTO。
     *
     * @param article 文章领域对象
     * @param author 作者领域对象
     * @return 文章应用 DTO
     */
    public static ArticleDTO toDTO(Article article, User author) {
        ArticleDTO dto = new ArticleDTO();
        dto.setId(article.getId().getValue());
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setContent(article.getContent());
        dto.setCoverUrl(article.getCoverUrl());
        dto.setCategory(article.getCategory());
        dto.setTags(article.getTags());
        dto.setStatus(article.getStatus().name());
        dto.setViewCount(article.getViewCount());
        dto.setLikeCount(article.getLikeCount());
        dto.setFavoriteCount(article.getFavoriteCount());
        dto.setCommentCount(article.getCommentCount());
        if (article.getPublishedAt() != null) {
            dto.setPublishedAt(FORMATTER.format(article.getPublishedAt()));
        }
        UserDTO authorDTO = UserAssembler.toDTO(author);
        dto.setAuthor(authorDTO);
        return dto;
    }
}
