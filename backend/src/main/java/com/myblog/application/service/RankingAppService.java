package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 排行榜应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class RankingAppService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;

    public RankingAppService(ArticleRepository articleRepository,
                             UserRepository userRepository,
                             UserFollowRepository userFollowRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
    }

    /**
     * 查询文章排行榜。
     *
     * @param limit 限制数量
     * @return 文章排行榜
     */
    public List<ArticleDTO> listArticleRankings(int limit) {
        List<Article> articles = articleRepository.findPublished(null, null, null, "hot");
        int maxSize = Math.min(Math.max(limit, 1), articles.size());
        List<ArticleDTO> items = new ArrayList<ArticleDTO>(maxSize);
        for (Article article : articles.subList(0, maxSize)) {
            User author = userRepository.findById(article.getAuthorId()).orElse(null);
            if (author != null) {
                items.add(ArticleAssembler.toDTO(article, author));
            }
        }
        return items;
    }

    /**
     * 查询作者排行榜。
     *
     * @param limit 限制数量
     * @param currentUserId 当前用户 ID
     * @return 作者排行榜
     */
    public List<AuthorRankingDTO> listAuthorRankings(int limit, Long currentUserId) {
        List<User> users = userRepository.findAll();
        List<Article> articles = articleRepository.findPublished(null, null, null, "latest");
        Map<Long, AuthorRankingDTO> rankingMap = new HashMap<Long, AuthorRankingDTO>();

        for (Article article : articles) {
            Long authorId = article.getAuthorId().getValue();
            AuthorRankingDTO dto = rankingMap.get(authorId);
            if (dto == null) {
                User author = findUser(users, authorId);
                if (author == null) {
                    continue;
                }
                dto = new AuthorRankingDTO();
                dto.setUser(UserAssembler.toDTO(author));
                dto.setFollowerCount(userFollowRepository.countFollowers(new UserId(authorId)));
                dto.setFollowed(currentUserId != null && userFollowRepository.exists(new UserId(currentUserId), new UserId(authorId)));
                rankingMap.put(authorId, dto);
            }
            dto.setArticleCount(dto.getArticleCount() + 1);
            dto.setTotalViewCount(dto.getTotalViewCount() + article.getViewCount());
            dto.setTotalLikeCount(dto.getTotalLikeCount() + article.getLikeCount());
        }

        List<AuthorRankingDTO> items = new ArrayList<AuthorRankingDTO>(rankingMap.values());
        Collections.sort(items, new Comparator<AuthorRankingDTO>() {
            @Override
            public int compare(AuthorRankingDTO left, AuthorRankingDTO right) {
                int compared = Long.compare(right.getTotalViewCount(), left.getTotalViewCount());
                if (compared != 0) {
                    return compared;
                }
                compared = Long.compare(right.getTotalLikeCount(), left.getTotalLikeCount());
                if (compared != 0) {
                    return compared;
                }
                compared = Long.compare(right.getArticleCount(), left.getArticleCount());
                if (compared != 0) {
                    return compared;
                }
                return Long.compare(left.getUser().getId(), right.getUser().getId());
            }
        });

        int maxSize = Math.min(Math.max(limit, 1), items.size());
        List<AuthorRankingDTO> result = new ArrayList<AuthorRankingDTO>(maxSize);
        for (int index = 0; index < maxSize; index++) {
            AuthorRankingDTO dto = items.get(index);
            dto.setRank(index + 1);
            result.add(dto);
        }
        return result;
    }

    private User findUser(List<User> users, Long userId) {
        for (User user : users) {
            if (user.getId().getValue().equals(userId)) {
                return user;
            }
        }
        return null;
    }
}
