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
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(UserAppService.class);

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

    /**
     * 更新用户个人资料。
     *
     * @param userId    用户 ID
     * @param nickname  昵称
     * @param avatarUrl 头像 URL
     * @param bio       个人简介
     * @param website   个人网站
     * @param github    GitHub 主页
     * @param twitter   Twitter 主页
     * @param location  所在地
     * @return 更新后的用户 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateProfile(Long userId, String nickname, String avatarUrl, String bio,
                                 String website, String github, String twitter, String location) {
        long _start = System.currentTimeMillis();
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.updateProfile(nickname, avatarUrl, bio);
        user.updateExtendedProfile(website, github, twitter, location);
        userRepository.save(user);
        UserDTO result = UserAssembler.toDTO(user);
        log.info("{} | {} 更新个人资料 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId, nickname),
            BizLogHelper.params("nickname", nickname, "bio", bio, "website", website),
            BizLogHelper.result("userId=" + userId),
            BizLogHelper.elapsed(_start));
        return result;
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
        long _start = System.currentTimeMillis();
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
        log.info("{} | {} 修改密码 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            BizLogHelper.params("userId", userId),
            BizLogHelper.result("changed=true"),
            BizLogHelper.elapsed(_start));
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
        long _start = System.currentTimeMillis();
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
        com.myblog.application.dto.UserDTO result = UserAssembler.toDTO(user);
        log.info("{} | {} 更换邮箱 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            BizLogHelper.params("newEmail", newEmail),
            BizLogHelper.result("userId=" + userId),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 忘记密码：生成重置 Token 并发送重置邮件。
     *
     * @param email 邮箱
     */
    @Transactional(rollbackFor = Exception.class)
    public String forgotPassword(String email) {
        long _start = System.currentTimeMillis();
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
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            "忘记密码",
            BizLogHelper.params("email", email),
            BizLogHelper.result("tokenIssued=true"),
            BizLogHelper.elapsed(_start));
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
        long _start = System.currentTimeMillis();
        User user = userRepository.findByPasswordResetToken(token)
            .orElseThrow(() -> new ApplicationException(ErrorCode.PARAM_ERROR, "重置链接无效或已过期"));
        user.validatePasswordResetToken(token);
        String newHash = passwordDomainService.encode(newPassword);
        user.changePassword(newHash);
        userRepository.save(user);
        log.info("{} | {} | 结果({}) | {}",
            BizLogHelper.trace(),
            "重置密码",
            BizLogHelper.result("changed=true"),
            BizLogHelper.elapsed(_start));
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

    /**
     * 获取用户主页资料（含统计数据和关注状态）。
     *
     * @param userId        被查看用户 ID
     * @param currentUserId 当前登录用户 ID（未登录时为 null）
     * @return 用户主页资料 DTO
     */
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

    /**
     * 分页查询指定用户已发布的文章列表。
     *
     * @param userId   用户 ID
     * @param page     页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 文章分页结果
     */
    public PageResult<ArticleDTO> pagePublishedArticles(Long userId, int page, int pageSize) {
        User author = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        List<Article> publishedArticles = articleRepository.findByAuthorId(
            userId,
            ArticleStatus.PUBLISHED.name(),
            null,
            page,
            pageSize
        );
        long total = articleRepository.countByAuthorId(userId, ArticleStatus.PUBLISHED.name(), null);
        return buildArticlePage(publishedArticles, author, page, pageSize, total);
    }

    /**
     * 查询指定用户最热门的已发布文章列表。
     *
     * @param userId 用户 ID
     * @param limit  最大返回数量
     * @return 热门文章列表
     */
    public List<ArticleDTO> listHotPublishedArticles(Long userId, int limit) {
        User author = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        return buildArticleItems(articleRepository.findHotPublishedByAuthorId(userId, limit), author);
    }

    /**
     * 分页查询当前登录用户自己的文章列表（支持状态和关键词过滤）。
     *
     * @param userId   用户 ID
     * @param page     页码（从 1 开始）
     * @param pageSize 每页数量
     * @param status   文章状态筛选（null 表示不限）
     * @param keyword  关键词搜索（null 表示不限）
     * @return 文章分页结果
     */
    public PageResult<ArticleDTO> pageMyArticles(Long userId, int page, int pageSize, String status, String keyword) {
        User author = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        String normalizedStatus = status == null || status.trim().isEmpty() ? null : status.trim();
        String normalizedKeyword = normalizeKeyword(keyword);
        List<Article> ownArticles = articleRepository.findByAuthorId(
            userId,
            normalizedStatus,
            normalizedKeyword,
            page,
            pageSize
        );
        long total = articleRepository.countByAuthorId(userId, normalizedStatus, normalizedKeyword);
        return buildArticlePage(ownArticles, author, page, pageSize, total);
    }

    /**
     * 获取当前用户文章创作台概览（包含各状态文章数量、累计数据及推荐行动）。
     *
     * @param userId 用户 ID
     * @return 文章创作台概览 DTO
     */
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

    /**
     * 判断给定角色是否为管理员。
     *
     * @param role 角色名称
     * @return true 表示是管理员，false 表示不是
     */
    public boolean isAdmin(String role) {
        return UserRole.ADMIN.name().equals(role);
    }

    /**
     * 搜索用户（支持用户名/昵称关键词搜索）。
     *
     * @param keyword       搜索关键词
     * @param page          页码（从 1 开始）
     * @param pageSize      每页数量
     * @param currentUserId 当前登录用户 ID（用于判断是否已关注，未登录时为 null）
     * @return 用户搜索结果分页
     */
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

    /**
     * 构建文章分页结果。
     *
     * @param source   文章列表
     * @param author   作者领域对象
     * @param page     页码
     * @param pageSize 每页数量
     * @param total    总数
     * @return 文章分页结果
     */
    private PageResult<ArticleDTO> buildArticlePage(List<Article> source, User author, int page, int pageSize, long total) {
        return new PageResult<ArticleDTO>(buildArticleItems(source, author), page, pageSize, total);
    }

    /**
     * 将文章领域对象列表转换为 DTO 列表。
     *
     * @param source 文章领域对象列表
     * @param author 作者领域对象
     * @return 文章 DTO 列表
     */
    private List<ArticleDTO> buildArticleItems(List<Article> source, User author) {
        List<ArticleDTO> items = new ArrayList<ArticleDTO>(source.size());
        for (Article article : source) {
            items.add(articleAssembler.toDTO(article, author));
        }
        return items;
    }

    /**
     * 安全转换为 int，null 时返回 0。
     *
     * @param value 数字值
     * @return int 值
     */
    private int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * 安全转换为 long，null 时返回 0L。
     *
     * @param value 数字值
     * @return long 值
     */
    private long safeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

    /**
     * 根据文章概览数据填充推荐行动建议。
     *
     * @param overviewDTO 文章创作台概览 DTO
     */
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

    /**
     * 构建密码重置链接。
     *
     * @param token 重置 Token
     * @return 完整的重置链接 URL
     */
    private String buildResetLink(String token) {
        String baseUrl = frontendBaseUrl == null || frontendBaseUrl.trim().isEmpty()
            ? "http://localhost:5173"
            : frontendBaseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/auth/reset-password?token=" + token;
    }

    /**
     * 规范化搜索关键词（去除首尾空格，空字符串转为 null）。
     *
     * @param keyword 原始关键词
     * @return 规范化后的关键词，无效时返回 null
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 导出当前用户文章列表为 CSV 格式。
     *
     * @param userId  用户 ID
     * @param status  文章状态筛选（null 表示不限）
     * @param keyword 关键词搜索（null 表示不限）
     * @return CSV 文件字节数组（UTF-8 编码）
     */
    public byte[] exportMyArticlesCsv(Long userId, String status, String keyword) {
        long _start = System.currentTimeMillis();
        PageResult<ArticleDTO> page = pageMyArticles(userId, 1, 10000, status, keyword);
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
        byte[] result = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "导出文章CSV",
            BizLogHelper.params("status", status, "keyword", keyword),
            BizLogHelper.result("size=" + result.length),
            BizLogHelper.elapsed(_start));
        return result;
    }
    /**
     * 对 CSV 字段进行转义，处理包含逗号、引号或换行的情况。
     *
     * @param v 原始字段值
     * @return 转义后的 CSV 字段
     */
    private String escapeCsvUser(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) return "\"" + v.replace("\"", "\"\"\"") + "\"";
        return v;
    }
}
