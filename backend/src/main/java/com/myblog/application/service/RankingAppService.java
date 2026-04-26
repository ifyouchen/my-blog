package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleStatsDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class RankingAppService {

    private static final Logger log = LoggerFactory.getLogger(RankingAppService.class);

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final ArticleAssembler articleAssembler;
    private final Executor taskExecutor;
    private final Cache<Integer, List<ArticleDTO>> articleRankingsCache;
    private final Cache<Integer, List<AuthorRankingDTO>> authorRankingsCache;

    public RankingAppService(ArticleRepository articleRepository,
                             UserRepository userRepository,
                             UserFollowRepository userFollowRepository,
                             ArticleAssembler articleAssembler,
                             Executor taskExecutor,
                             @Qualifier("articleRankingsCache") Cache<Integer, List<ArticleDTO>> articleRankingsCache,
                             @Qualifier("authorRankingsCache") Cache<Integer, List<AuthorRankingDTO>> authorRankingsCache) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.articleAssembler = articleAssembler;
        this.taskExecutor = taskExecutor;
        this.articleRankingsCache = articleRankingsCache;
        this.authorRankingsCache = authorRankingsCache;
    }

    public List<ArticleDTO> listArticleRankings(int limit) {
        log.info("Fetching article rankings, limit={}", limit);
        List<ArticleDTO> cached = articleRankingsCache.getIfPresent(limit);
        if (cached != null) {
            return cached;
        }
        List<Article> articles = articleRepository.findPublishedWithLimit(null, null, null, "hot", limit, 0);

        if (articles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> authorIds = articles.stream()
            .map(a -> a.getAuthorId().getValue())
            .distinct()
            .collect(Collectors.toList());

        CompletableFuture<Map<Long, User>> authorFuture = CompletableFuture.supplyAsync(() ->
            userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u)), taskExecutor);

        Map<Long, User> authorMap = authorFuture.join();

        List<ArticleDTO> items = new ArrayList<>(articles.size());
        for (Article article : articles) {
            User author = authorMap.get(article.getAuthorId().getValue());
            if (author != null) {
                items.add(articleAssembler.toDTO(article, author));
            }
        }
        articleRankingsCache.put(limit, items);
        return items;
    }

    public List<AuthorRankingDTO> listAuthorRankings(int limit, Long currentUserId) {
        log.info("Fetching author rankings, limit={}, currentUserId={}", limit, currentUserId);
        List<AuthorRankingDTO> cachedBase = authorRankingsCache.getIfPresent(limit);
        if (cachedBase != null) {
            return applyFollowStatus(copyAuthorRankings(cachedBase), currentUserId);
        }
        List<AuthorArticleStatsDO> statsList = articleRepository.findAuthorArticleStats(limit);

        if (statsList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> authorIds = statsList.stream()
            .map(AuthorArticleStatsDO::getAuthorId)
            .collect(Collectors.toList());

        CompletableFuture<Map<Long, User>> authorFuture = CompletableFuture.supplyAsync(() ->
            userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(u -> u.getId().getValue(), u -> u)), taskExecutor);

        CompletableFuture<Map<Long, Integer>> followerCountFuture = CompletableFuture.supplyAsync(() ->
            userFollowRepository.countFollowersBatch(authorIds), taskExecutor);

        CompletableFuture<Map<Long, Boolean>> followStatusFuture = CompletableFuture.supplyAsync(() ->
            buildFollowStatusMap(currentUserId, authorIds), taskExecutor);

        Map<Long, User> authorMap = authorFuture.join();
        Map<Long, Integer> followerCountMap = followerCountFuture.join();
        Map<Long, Boolean> followStatusMap = followStatusFuture.join();

        List<AuthorRankingDTO> result = new ArrayList<>(statsList.size());
        int rank = 1;
        for (AuthorArticleStatsDO stats : statsList) {
            User author = authorMap.get(stats.getAuthorId());
            if (author == null) {
                continue;
            }
            AuthorRankingDTO dto = new AuthorRankingDTO();
            dto.setUser(UserAssembler.toDTO(author));
            dto.setArticleCount(stats.getArticleCount() != null ? stats.getArticleCount() : 0);
            dto.setTotalViewCount(stats.getTotalViews() != null ? stats.getTotalViews() : 0L);
            dto.setTotalLikeCount(stats.getTotalLikes() != null ? stats.getTotalLikes() : 0L);
            dto.setFollowerCount(followerCountMap.getOrDefault(stats.getAuthorId(), 0));
            dto.setFollowed(followStatusMap.getOrDefault(stats.getAuthorId(), false));
            dto.setRank(rank++);
            result.add(dto);
        }
        authorRankingsCache.put(limit, copyAuthorRankings(result));
        return applyFollowStatus(result, currentUserId);
    }

    private Map<Long, Boolean> buildFollowStatusMap(Long currentUserId, List<Long> authorIds) {
        if (currentUserId == null || authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> followedAuthorIds = userFollowRepository.findFollowingUserIdsIn(new UserId(currentUserId), authorIds);
        Map<Long, Boolean> result = new HashMap<Long, Boolean>();
        for (Long authorId : authorIds) {
            result.put(authorId, false);
        }
        for (Long authorId : followedAuthorIds) {
            result.put(authorId, true);
        }
        return result;
    }

    private List<AuthorRankingDTO> applyFollowStatus(List<AuthorRankingDTO> items, Long currentUserId) {
        if (items.isEmpty()) {
            return items;
        }
        List<Long> authorIds = items.stream()
                .map(item -> item.getUser().getId())
                .collect(Collectors.toList());
        Map<Long, Boolean> followStatusMap = buildFollowStatusMap(currentUserId, authorIds);
        for (AuthorRankingDTO item : items) {
            item.setFollowed(followStatusMap.getOrDefault(item.getUser().getId(), false));
        }
        return items;
    }

    private List<AuthorRankingDTO> copyAuthorRankings(List<AuthorRankingDTO> source) {
        List<AuthorRankingDTO> copies = new ArrayList<AuthorRankingDTO>(source.size());
        for (AuthorRankingDTO item : source) {
            AuthorRankingDTO dto = new AuthorRankingDTO();
            dto.setRank(item.getRank());
            dto.setUser(copyUser(item.getUser()));
            dto.setArticleCount(item.getArticleCount());
            dto.setTotalViewCount(item.getTotalViewCount());
            dto.setTotalLikeCount(item.getTotalLikeCount());
            dto.setFollowerCount(item.getFollowerCount());
            dto.setFollowed(item.isFollowed());
            copies.add(dto);
        }
        return copies;
    }

    private UserDTO copyUser(UserDTO source) {
        UserDTO dto = new UserDTO();
        dto.setId(source.getId());
        dto.setUsername(source.getUsername());
        dto.setEmail(source.getEmail());
        dto.setNickname(source.getNickname());
        dto.setAvatarUrl(source.getAvatarUrl());
        dto.setBio(source.getBio());
        dto.setRole(source.getRole());
        return dto;
    }
}
