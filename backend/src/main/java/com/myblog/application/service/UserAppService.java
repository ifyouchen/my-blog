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
import com.myblog.domain.service.PasswordDomainService;
import com.myblog.infrastructure.repository.persistence.entity.AuthorArticleMetricsDO;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.beans.factory.annotation.Value;
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
    private final PasswordDomainService passwordDomainService;
    private final EmailQueueAppService emailQueueAppService;
    private final String frontendBaseUrl;

    public UserAppService(UserRepository userRepository,
                          ArticleRepository articleRepository,
                          UserFollowRepository userFollowRepository,
                          ArticleAssembler articleAssembler,
                          PasswordDomainService passwordDomainService,
                          EmailQueueAppService emailQueueAppService,
                          @Value("${my-blog.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.userFollowRepository = userFollowRepository;
        this.articleAssembler = articleAssembler;
        this.passwordDomainService = passwordDomainService;
        this.emailQueueAppService = emailQueueAppService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateProfile(Long userId, String nickname, String avatarUrl, String bio,
                                 String website, String github, String twitter, String location) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.updateProfile(nickname, avatarUrl, bio);
        user.updateExtendedProfile(website, github, twitter, location);
        userRepository.save(user);
        return UserAssembler.toDTO(user);
    }

    /**
     * 修改密码。
     *
     * @param userId 用户 ID
     * @param currentPassword 当前密码
     * @param newPassword 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (!passwordDomainService.matches(currentPassword, user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "当前密码错误");
        }
        if (passwordDomainService.matches(newPassword, user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "新密码不能与当前密码相同");
        }
        String newHash = passwordDomainService.encode(newPassword);
        user.changePassword(newHash);
        userRepository.save(user);
    }

    /**
     * 绑定/更换邮箱（需验证当前密码）。
     *
     * @param userId 用户 ID
     * @param newEmail 新邮箱
     * @param password 当前密码（确认身份）
     */
    @Transactional(rollbackFor = Exception.class)
    public com.myblog.application.dto.UserDTO changeEmail(Long userId, String newEmail, String password) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (!passwordDomainService.matches(password, user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "密码错误，无法修改邮箱");
        }
        java.util.Optional<User> existing = userRepository.findByEmail(newEmail);
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "该邮箱已被其他账号绑定");
        }
        user.changeEmail(newEmail);
        userRepository.save(user);
        return UserAssembler.toDTO(user);
    }

    /**
     * 忘记密码：生成重置 Token 并发送重置邮件。
     *
     * @param email 邮箱
     */
    @Transactional(rollbackFor = Exception.class)
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "该邮箱未注册"));
        String token = user.generatePasswordResetToken();
        userRepository.save(user);
        try {
            emailQueueAppService.enqueuePasswordReset(
                user.getEmail().getValue(),
                user.getUsername(),
                buildResetLink(token)
            );
        } catch (ApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApplicationException(ErrorCode.SYSTEM_ERROR, "重置邮件提交失败，请稍后重试");
        }
        return token;
    }

    /**
     * 通过 Token 重置密码。
     *
     * @param token 重置 Token
     * @param newPassword 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
            .orElseThrow(() -> new ApplicationException(ErrorCode.PARAM_ERROR, "重置链接无效或已过期"));
        user.validatePasswordResetToken(token);
        String newHash = passwordDomainService.encode(newPassword);
        user.changePassword(newHash);
        userRepository.save(user);
    }

    /**
     * 获取用户安全信息。
     *
     * @param userId 用户 ID
     * @return 用户 DTO（含 lastLoginAt/lastLoginIp）
     */
    public UserDTO getSecurityInfo(Long userId) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
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
            overviewDTO.setLatestArticleId(latestArticle.getId().getValue());
            overviewDTO.setLatestArticleTitle(latestArticle.getTitle());
            overviewDTO.setLatestArticleStatus(latestArticle.getStatus().name());
            overviewDTO.setLatestUpdatedAt(DATETIME_FORMATTER.format(latestArticle.getUpdatedAt()));
        }
        populateRecommendedAction(overviewDTO);
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

    private void populateRecommendedAction(MyArticleOverviewDTO overviewDTO) {
        if (overviewDTO.getTotalCount() <= 0) {
            overviewDTO.setRecommendedActionType("CREATE");
            overviewDTO.setRecommendedActionText("开始写第一篇文章");
            overviewDTO.setRecommendedActionHint("先完成一篇草稿，创作台会在这里持续给你下一步建议。");
            overviewDTO.setRecommendedActionRoute("/editor/new");
            return;
        }
        if (overviewDTO.getDraftCount() > 0) {
            overviewDTO.setRecommendedActionType("DRAFT");
            overviewDTO.setRecommendedActionText("继续处理草稿");
            overviewDTO.setRecommendedActionHint("当前还有 " + overviewDTO.getDraftCount() + " 篇草稿待整理，优先补全后再发布。");
            overviewDTO.setRecommendedActionRoute("/dashboard/articles?status=DRAFT");
            return;
        }
        if (overviewDTO.getOfflineCount() > 0) {
            overviewDTO.setRecommendedActionType("OFFLINE");
            overviewDTO.setRecommendedActionText("检查已下架内容");
            overviewDTO.setRecommendedActionHint("有 " + overviewDTO.getOfflineCount() + " 篇文章处于下架状态，可以评估是否重新发布。");
            overviewDTO.setRecommendedActionRoute("/dashboard/articles?status=OFFLINE");
            return;
        }
        overviewDTO.setRecommendedActionType("PUBLISHED");
        overviewDTO.setRecommendedActionText("查看已发布文章");
        overviewDTO.setRecommendedActionHint("你的内容已经在线，继续优化标题、摘要和互动表现会更有效。");
        overviewDTO.setRecommendedActionRoute("/dashboard/articles?status=PUBLISHED");
    }

    private String buildResetLink(String token) {
        String baseUrl = frontendBaseUrl == null || frontendBaseUrl.trim().isEmpty()
            ? "http://localhost:5173"
            : frontendBaseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/auth/reset-password?token=" + token;
    }


    public byte[] exportMyArticlesCsv(Long userId, String status) {
        PageResult<ArticleDTO> page = pageMyArticles(userId, 1, 10000, status);
        StringBuilder sb = new StringBuilder();
        sb.append("id,title,category,status,viewCount,likeCount,commentCount,publishedAt,createdAt\n");
        for (ArticleDTO a : page.getItems()) {
            sb.append(a.getId()).append(",");
            sb.append(escapeCsvUser(a.getTitle())).append(",");
            sb.append(escapeCsvUser(a.getCategory())).append(",");
            sb.append(escapeCsvUser(a.getStatus())).append(",");
            sb.append(a.getViewCount()).append(",");
            sb.append(a.getLikeCount()).append(",");
            sb.append(a.getCommentCount()).append(",");
            sb.append(escapeCsvUser(a.getPublishedAt()!=null?a.getPublishedAt().toString():"")).append(",");
            sb.append(escapeCsvUser(a.getUpdatedAt()!=null?a.getUpdatedAt():"" )).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    private String escapeCsvUser(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) return "\"" + v.replace("\"", "\"\"\"") + "\"";
        return v;
    }
}
