package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.assembler.UserAssembler;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.MyArticleOverviewDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.dto.UserProfileDTO;
import com.myblog.application.dto.UserSearchDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserFollowRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleMetricsDO;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class UserAppService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final UserFollowRepository userFollowRepository;
    private final ArticleAssembler articleAssembler;

    public UserAppService(UserRepository userRepository,
                          ArticleRepository articleRepository,
                          UserFollowRepository userFollowRepository,
                          ArticleAssembler articleAssembler) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.userFollowRepository = userFollowRepository;
        this.articleAssembler = articleAssembler;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateProfile(Long userId, String nickname, String avatarUrl, String bio) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.updateProfile(nickname, avatarUrl, bio);
        userRepository.save(user);
        return UserAssembler.toDTO(user);
    }

    public UserProfileDTO getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        AuthorArticleMetricsDO metrics = articleRepository.summarizeByAuthor(userId, ArticleStatus.PUBLISHED.name());
        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setUser(UserAssembler.toDTO(user));
        profileDTO.setArticleCount(safeInt(metrics != null ? metrics.getArticleCount() : null));
        profileDTO.setTotalViewCount(safeLong(metrics != null ? metrics.getTotalViews() : null));
        profileDTO.setTotalLikeCount(safeLong(metrics != null ? metrics.getTotalLikes() : null));
        profileDTO.setFollowerCount(userFollowRepository.countFollowers(new UserId(userId)));
        profileDTO.setFollowingCount(userFollowRepository.countFollowing(new UserId(userId)));
        profileDTO.setFollowing(
            currentUserId != null
                && !currentUserId.equals(userId)
                && userFollowRepository.exists(new UserId(currentUserId), new UserId(userId))
        );
        return profileDTO;
    }

    public PageResult<ArticleDTO> pagePublishedArticles(Long userId, int page, int pageSize) {
        User author = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        List<Article> publishedArticles = articleRepository.findByAuthorId(
            userId,
            ArticleStatus.PUBLISHED.name(),
            page,
            pageSize
        );
        long total = articleRepository.countByAuthorId(userId, ArticleStatus.PUBLISHED.name());
        return buildArticlePage(publishedArticles, author, page, pageSize, total);
    }

    public List<ArticleDTO> listHotPublishedArticles(Long userId, int limit) {
        User author = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        return buildArticleItems(articleRepository.findHotPublishedByAuthorId(userId, limit), author);
    }

    public PageResult<ArticleDTO> pageMyArticles(Long userId, int page, int pageSize, String status) {
        User author = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        String normalizedStatus = status == null || status.trim().isEmpty() ? null : status.trim();
        List<Article> ownArticles = articleRepository.findByAuthorId(userId, normalizedStatus, page, pageSize);
        long total = articleRepository.countByAuthorId(userId, normalizedStatus);
        return buildArticlePage(ownArticles, author, page, pageSize, total);
    }

    public MyArticleOverviewDTO getMyArticleOverview(Long userId) {
        AuthorArticleMetricsDO metrics = articleRepository.summarizeByAuthor(userId, null);
        Article latestArticle = articleRepository.findLatestByAuthorId(userId).orElse(null);
        MyArticleOverviewDTO overviewDTO = new MyArticleOverviewDTO();
        overviewDTO.setTotalCount(safeInt(metrics != null ? metrics.getArticleCount() : null));
        overviewDTO.setDraftCount(safeInt(metrics != null ? metrics.getDraftCount() : null));
        overviewDTO.setPublishedCount(safeInt(metrics != null ? metrics.getPublishedCount() : null));
        overviewDTO.setOfflineCount(safeInt(metrics != null ? metrics.getOfflineCount() : null));
        overviewDTO.setDeletedCount(safeInt(metrics != null ? metrics.getDeletedCount() : null));
        overviewDTO.setTotalViewCount(safeLong(metrics != null ? metrics.getTotalViews() : null));
        overviewDTO.setTotalLikeCount(safeLong(metrics != null ? metrics.getTotalLikes() : null));
        overviewDTO.setTotalFavoriteCount(safeLong(metrics != null ? metrics.getTotalFavorites() : null));
        overviewDTO.setTotalCommentCount(safeLong(metrics != null ? metrics.getTotalComments() : null));
        if (latestArticle != null) {
            overviewDTO.setLatestArticleTitle(latestArticle.getTitle());
            overviewDTO.setLatestUpdatedAt(DATETIME_FORMATTER.format(latestArticle.getUpdatedAt()));
        }
        return overviewDTO;
    }

    public boolean isAdmin(String role) {
        return UserRole.ADMIN.name().equals(role);
    }

    public PageResult<UserSearchDTO> searchUsers(String keyword, int page, int pageSize, Long currentUserId) {
        List<User> users = userRepository.searchUsers(keyword, "followers", page, pageSize);
        long total = userRepository.countSearchUsers(keyword);

        if (users.isEmpty()) {
            return new PageResult<>(new ArrayList<UserSearchDTO>(), page, pageSize, total);
        }

        // 批量查询粉丝数、文章数、关注状态，解决 N+1 问题
        List<Long> userIds = users.stream()
            .map(u -> u.getId().getValue())
            .collect(Collectors.toList());

        Map<Long, Integer> followerCountMap = userRepository.countFollowersBatchByIds(userIds);
        Map<Long, Integer> articleCountMap = userRepository.countPublishedArticlesBatchByIds(userIds);

        Set<Long> followedUserIds = java.util.Collections.emptySet();
        if (currentUserId != null) {
            List<Long> followedList = userFollowRepository.findFollowingUserIdsIn(
                new UserId(currentUserId), userIds
            );
            followedUserIds = new java.util.HashSet<>(followedList);
        }

        final Set<Long> finalFollowedUserIds = followedUserIds;
        List<UserSearchDTO> dtos = new ArrayList<>(users.size());
        for (User user : users) {
            Long uid = user.getId().getValue();
            UserSearchDTO dto = new UserSearchDTO();
            dto.setId(uid);
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setAvatarUrl(user.getAvatarUrl());
            dto.setBio(user.getBio());
            dto.setFollowerCount(followerCountMap.getOrDefault(uid, 0));
            dto.setPublishedArticleCount(articleCountMap.getOrDefault(uid, 0));
            dto.setFollowed(finalFollowedUserIds.contains(uid));
            dtos.add(dto);
        }

        return new PageResult<>(dtos, page, pageSize, total);
    }

    private PageResult<ArticleDTO> buildArticlePage(List<Article> source, User author, int page, int pageSize, long total) {
        return new PageResult<ArticleDTO>(buildArticleItems(source, author), page, pageSize, total);
    }

    private List<ArticleDTO> buildArticleItems(List<Article> source, User author) {
        List<ArticleDTO> items = new ArrayList<ArticleDTO>(source.size());
        for (Article article : source) {
            items.add(articleAssembler.toDTO(article, author));
        }
        return items;
    }

    private int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private long safeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
