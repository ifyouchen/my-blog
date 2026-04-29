package com.myblog.application.service;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.ColumnSubscriptionRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 推荐应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class RecommendationAppService {

    private final UserRepository userRepository;
    private final ColumnRepository columnRepository;
    private final ArticleRepository articleRepository;
    private final ColumnSubscriptionRepository columnSubscriptionRepository;
    private final UserFollowRepository userFollowRepository;
    private final ArticleAssembler articleAssembler;

    public RecommendationAppService(UserRepository userRepository,
                                    ColumnRepository columnRepository,
                                    ArticleRepository articleRepository,
                                    ColumnSubscriptionRepository columnSubscriptionRepository,
                                    UserFollowRepository userFollowRepository,
                                    ArticleAssembler articleAssembler) {
        this.userRepository = userRepository;
        this.columnRepository = columnRepository;
        this.articleRepository = articleRepository;
        this.columnSubscriptionRepository = columnSubscriptionRepository;
        this.userFollowRepository = userFollowRepository;
        this.articleAssembler = articleAssembler;
    }

    /**
     * 推荐作者列表。
     */
    public List<UserDTO> listRecommendedAuthors(int limit, Long currentUserId) {
        List<User> users = userRepository.findRecommended(limit);
        List<UserDTO> items = new ArrayList<>(users.size());
        for (User user : users) {
            UserDTO dto = UserAssembler.toDTO(user);
            if (currentUserId != null) {
                dto.setFollowed(userFollowRepository.exists(new UserId(currentUserId), user.getId()));
            }
            items.add(dto);
        }
        return items;
    }

    /**
     * 推荐专栏列表。
     */
    public List<ColumnDTO> listRecommendedColumns(int limit, Long currentUserId) {
        List<Column> columns = columnRepository.findRecommended(limit);
        List<ColumnDTO> items = new ArrayList<>(columns.size());
        for (Column column : columns) {
            ColumnDTO dto = toColumnDTO(column, currentUserId);
            items.add(dto);
        }
        return items;
    }

    /**
     * 推荐/精选文章列表。
     */
    public List<ArticleDTO> listFeaturedArticles(int page, int pageSize) {
        List<Article> articles = articleRepository.findFeatured(page, pageSize);
        List<ArticleDTO> items = new ArrayList<>(articles.size());
        for (Article article : articles) {
            User author = userRepository.findById(article.getAuthorId()).orElse(null);
            if (author == null) {
                continue;
            }
            items.add(articleAssembler.toDTO(article, author));
        }
        return items;
    }

    private ColumnDTO toColumnDTO(Column column, Long currentUserId) {
        User author = userRepository.findById(column.getAuthorId())
            .orElse(null);
        ColumnDTO dto = new ColumnDTO();
        dto.setId(column.getId().getValue());
        dto.setTitle(column.getTitle());
        dto.setSummary(column.getSummary());
        dto.setCoverUrl(column.getCoverUrl());
        dto.setSubscriberCount(column.getSubscriberCount());
        dto.setArticleCount(column.getArticleCount());
        dto.setSubscribed(currentUserId != null && columnSubscriptionRepository.exists(column.getId(), new UserId(currentUserId)));
        if (author != null) {
            dto.setAuthor(UserAssembler.toDTO(author));
        }
        return dto;
    }
}
