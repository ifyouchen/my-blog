package com.myblog.interfaces.rest.mapper;

import com.myblog.application.command.CreateArticleCommand;
import com.myblog.application.command.CreateCommentCommand;
import com.myblog.application.command.LoginCommand;
import com.myblog.application.command.RegisterCommand;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ArticleSummaryDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.AuthDTO;
import com.myblog.application.dto.CommentDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.dto.UserProfileDTO;
import com.myblog.interfaces.rest.dto.request.CreateArticleRequest;
import com.myblog.interfaces.rest.dto.request.CreateCommentRequest;
import com.myblog.interfaces.rest.dto.request.LoginRequest;
import com.myblog.interfaces.rest.dto.request.RegisterRequest;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.ArticleSummaryResponse;
import com.myblog.interfaces.rest.dto.response.AuthorRankingResponse;
import com.myblog.interfaces.rest.dto.response.AuthResponse;
import com.myblog.interfaces.rest.dto.response.ColumnResponse;
import com.myblog.interfaces.rest.dto.response.CommentResponse;
import com.myblog.interfaces.rest.dto.response.UserResponse;
import com.myblog.interfaces.rest.dto.response.UserProfileResponse;
import org.springframework.stereotype.Component;

/**
 * REST DTO 转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class RestDtoMapper {

    /**
     * 将注册请求转换为注册命令。
     *
     * @param request 注册请求
     * @return 注册命令
     */
    public RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterCommand(request.getUsername(), request.getEmail(),
            request.getPassword(), request.getInviteCode(), request.getEmailCode());
    }

    /**
     * 将登录请求转换为登录命令。
     *
     * @param request 登录请求
     * @return 登录命令
     */
    public LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.getAccount(), request.getPassword());
    }

    /**
     * 将登录请求转换为登录命令。
     *
     * @param request 登录请求
     * @param loginIp 登录 IP
     * @return 登录命令
     */
    public LoginCommand toCommand(LoginRequest request, String loginIp) {
        return new LoginCommand(request.getAccount(), request.getPassword(), loginIp);
    }

    /**
     * 将创建文章请求转换为创建文章命令。
     *
     * @param request 创建文章请求
     * @param authorId 作者 ID
     * @return 创建文章命令
     */
    public CreateArticleCommand toCommand(CreateArticleRequest request, Long authorId) {
        CreateArticleCommand command = new CreateArticleCommand();
        command.setAuthorId(authorId);
        command.setTitle(request.getTitle());
        command.setSummary(request.getSummary());
        command.setContent(request.getContent());
        command.setCoverUrl(request.getCoverUrl());
        command.setCategory(request.getCategory());
        command.setTags(request.getTags());
        command.setStatus(request.getStatus());
        command.setSlug(request.getSlug());
        command.setSeoTitle(request.getSeoTitle());
        command.setSeoDescription(request.getSeoDescription());
        command.setScheduledPublishAt(request.getScheduledPublishAt());
        return command;
    }

    /**
     * 将创建评论请求转换为创建评论命令。
     *
     * @param request 创建评论请求
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 创建评论命令
     */
    public CreateCommentCommand toCommand(CreateCommentRequest request, Long articleId, Long userId) {
        CreateCommentCommand command = new CreateCommentCommand();
        command.setArticleId(articleId);
        command.setUserId(userId);
        command.setRootCommentId(request.getRootCommentId());
        command.setParentId(request.getParentId());
        command.setContent(request.getContent());
        return command;
    }

    /**
     * 将认证 DTO 转换为响应。
     *
     * @param dto 认证 DTO
     * @return 认证响应
     */
    public AuthResponse toResponse(AuthDTO dto) {
        return new AuthResponse(dto.getToken(), toResponse(dto.getUser()));
    }

    /**
     * 将用户 DTO 转换为公开响应，隐藏邮箱和登录痕迹。
     *
     * @param dto 用户 DTO
     * @return 公开用户响应
     */
    public UserResponse toPublicResponse(UserDTO dto) {
        UserResponse response = toBaseUserResponse(dto);
        response.setEmail(null);
        response.setLastLoginAt(null);
        response.setLastLoginIp(null);
        response.setFollowed(Boolean.TRUE.equals(dto.getFollowed()));
        return response;
    }

    /**
     * 将用户 DTO 转换为响应。
     *
     * @param dto 用户 DTO
     * @return 用户响应
     */
    public UserResponse toResponse(UserDTO dto) {
        UserResponse response = toBaseUserResponse(dto);
        response.setEmail(dto.getEmail());
        response.setLastLoginAt(dto.getLastLoginAt());
        response.setLastLoginIp(dto.getLastLoginIp());
        return response;
    }

    private UserResponse toBaseUserResponse(UserDTO dto) {
        UserResponse response = new UserResponse();
        response.setId(dto.getId());
        response.setUsername(dto.getUsername());
        response.setNickname(dto.getNickname());
        response.setAvatarUrl(dto.getAvatarUrl());
        response.setBio(dto.getBio());
        response.setWebsite(dto.getWebsite());
        response.setGithub(dto.getGithub());
        response.setTwitter(dto.getTwitter());
        response.setLocation(dto.getLocation());
        response.setRole(dto.getRole());
        response.setStatus(dto.getStatus());
        response.setFollowed(dto.getFollowed());
        response.setFollowerCount(dto.getFollowerCount());
        response.setArticleCount(dto.getArticleCount());
        response.setTotalLikeCount(dto.getTotalLikeCount());
        return response;
    }

    /**
     * 将用户主页 DTO 转换为响应。
     *
     * @param dto 用户主页 DTO
     * @return 用户主页响应
     */
    public UserProfileResponse toResponse(UserProfileDTO dto) {
        return toUserProfileResponse(dto, false);
    }

    /**
     * 将用户主页 DTO 转换为公开响应。
     *
     * @param dto 用户主页 DTO
     * @return 公开用户主页响应
     */
    public UserProfileResponse toPublicResponse(UserProfileDTO dto) {
        return toUserProfileResponse(dto, true);
    }

    private UserProfileResponse toUserProfileResponse(UserProfileDTO dto, boolean publicUser) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUser(publicUser ? toPublicResponse(dto.getUser()) : toResponse(dto.getUser()));
        response.setArticleCount(dto.getArticleCount());
        response.setTotalViewCount(dto.getTotalViewCount());
        response.setTotalLikeCount(dto.getTotalLikeCount());
        response.setFollowerCount(dto.getFollowerCount());
        response.setFollowingCount(dto.getFollowingCount());
        response.setFollowing(dto.isFollowing());
        return response;
    }

    /**
     * 将专栏 DTO 转换为响应。
     *
     * @param dto 专栏 DTO
     * @return 专栏响应
     */
    public ColumnResponse toResponse(ColumnDTO dto) {
        return toColumnResponse(dto, false);
    }

    /**
     * 将专栏 DTO 转换为公开响应。
     *
     * @param dto 专栏 DTO
     * @return 公开专栏响应
     */
    public ColumnResponse toPublicResponse(ColumnDTO dto) {
        return toColumnResponse(dto, true);
    }

    private ColumnResponse toColumnResponse(ColumnDTO dto, boolean publicUser) {
        ColumnResponse response = new ColumnResponse();
        response.setId(dto.getId());
        response.setTitle(dto.getTitle());
        response.setSummary(dto.getSummary());
        response.setCoverUrl(dto.getCoverUrl());
        response.setSubscriberCount(dto.getSubscriberCount());
        response.setArticleCount(dto.getArticleCount());
        response.setSubscribed(dto.isSubscribed());
        response.setIntro(dto.getIntro());
        response.setDifficulty(dto.getDifficulty());
        response.setEstimatedMinutes(dto.getEstimatedMinutes());
        response.setSourceType(dto.getSourceType());
        response.setSourceNote(dto.getSourceNote());
        response.setRecommended(dto.isRecommended());
        response.setRecommendWeight(dto.getRecommendWeight());
        response.setProgress(dto.getProgress());
        response.setOutline(dto.getOutline());
        response.setNextArticle(dto.getNextArticle() == null ? null : toPublicResponse(dto.getNextArticle()));
        if (dto.getAuthor() != null) {
            response.setAuthor(publicUser ? toPublicResponse(dto.getAuthor()) : toResponse(dto.getAuthor()));
        }
        return response;
    }

    /**
     * 将作者排行榜 DTO 转换为响应。
     *
     * @param dto 作者排行榜 DTO
     * @return 作者排行榜响应
     */
    public AuthorRankingResponse toResponse(AuthorRankingDTO dto) {
        return toAuthorRankingResponse(dto, false);
    }

    /**
     * 将作者排行榜 DTO 转换为公开响应。
     *
     * @param dto 作者排行榜 DTO
     * @return 公开作者排行榜响应
     */
    public AuthorRankingResponse toPublicResponse(AuthorRankingDTO dto) {
        return toAuthorRankingResponse(dto, true);
    }

    private AuthorRankingResponse toAuthorRankingResponse(AuthorRankingDTO dto, boolean publicUser) {
        AuthorRankingResponse response = new AuthorRankingResponse();
        response.setRank(dto.getRank());
        response.setUser(publicUser ? toPublicResponse(dto.getUser()) : toResponse(dto.getUser()));
        response.setArticleCount(dto.getArticleCount());
        response.setTotalViewCount(dto.getTotalViewCount());
        response.setTotalLikeCount(dto.getTotalLikeCount());
        response.setFollowerCount(dto.getFollowerCount());
        response.setFollowed(dto.isFollowed());
        response.setTopArticle(toArticleSummaryResponse(dto.getTopArticle()));
        return response;
    }

    private ArticleSummaryResponse toArticleSummaryResponse(ArticleSummaryDTO dto) {
        if (dto == null) {
            return null;
        }
        ArticleSummaryResponse response = new ArticleSummaryResponse();
        response.setId(dto.getId());
        response.setTitle(dto.getTitle());
        response.setSlug(dto.getSlug());
        return response;
    }

    /**
     * 将文章 DTO 转换为响应。
     *
     * @param dto 文章 DTO
     * @return 文章响应
     */
    public ArticleResponse toResponse(ArticleDTO dto) {
        return toArticleResponse(dto, false);
    }

    /**
     * 将文章 DTO 转换为公开响应。
     *
     * @param dto 文章 DTO
     * @return 公开文章响应
     */
    public ArticleResponse toPublicResponse(ArticleDTO dto) {
        return toArticleResponse(dto, true);
    }

    private ArticleResponse toArticleResponse(ArticleDTO dto, boolean publicUser) {
        ArticleResponse response = new ArticleResponse();
        response.setId(dto.getId());
        response.setTitle(dto.getTitle());
        response.setSummary(dto.getSummary());
        response.setContent(dto.getContent());
        response.setWordCount(dto.getWordCount());
        response.setCoverUrl(dto.getCoverUrl());
        response.setCategory(dto.getCategory());
        response.setTags(dto.getTags());
        response.setStatus(dto.getStatus());
        response.setViewCount(dto.getViewCount());
        response.setLikeCount(dto.getLikeCount());
        response.setFavoriteCount(dto.getFavoriteCount());
        response.setCommentCount(dto.getCommentCount());
        response.setPublishedAt(dto.getPublishedAt());
        response.setUpdatedAt(dto.getUpdatedAt());
        response.setFavoritedAt(dto.getFavoritedAt());
        response.setLiked(dto.isLiked());
        response.setFavorited(dto.isFavorited());
        response.setFeatured(dto.isFeatured());
        response.setFeaturedAt(dto.getFeaturedAt());
        response.setFeatureWeight(dto.getFeatureWeight());
        response.setSlug(dto.getSlug());
        response.setSeoTitle(dto.getSeoTitle());
        response.setSeoDescription(dto.getSeoDescription());
        response.setScheduledPublishAt(dto.getScheduledPublishAt());
        response.setOfflineReason(dto.getOfflineReason());
        response.setWarnFlag(dto.isWarnFlag());
        if (dto.getAuthor() != null) {
            response.setAuthor(publicUser ? toPublicResponse(dto.getAuthor()) : toResponse(dto.getAuthor()));
        }
        return response;
    }

    /**
     * 将评论 DTO 转换为响应。
     *
     * @param dto 评论 DTO
     * @return 评论响应
     */
    public CommentResponse toResponse(CommentDTO dto) {
        return toCommentResponse(dto, false);
    }

    /**
     * 将评论 DTO 转换为公开响应。
     *
     * @param dto 评论 DTO
     * @return 公开评论响应
     */
    public CommentResponse toPublicResponse(CommentDTO dto) {
        return toCommentResponse(dto, true);
    }

    private CommentResponse toCommentResponse(CommentDTO dto, boolean publicUser) {
        CommentResponse response = new CommentResponse();
        response.setId(dto.getId());
        response.setArticleId(dto.getArticleId());
        response.setUserId(dto.getUserId());
        response.setRootCommentId(dto.getRootCommentId());
        response.setParentId(dto.getParentId());
        response.setContent(dto.getContent());
        response.setStatus(dto.getStatus());
        response.setReplyCount(dto.getReplyCount());
        response.setLikeCount(dto.getLikeCount());
        response.setLiked(dto.getLiked());
        response.setPinned(dto.getPinned());
        response.setCanDelete(dto.getCanDelete());
        response.setCanPin(dto.getCanPin());
        response.setAuthor(dto.getAuthor());
        response.setCreatedAt(dto.getCreatedAt());
        response.setEditedAt(dto.getEditedAt());
        response.setEditCount(dto.getEditCount());
        response.setCanEdit(dto.getCanEdit());
        if (dto.getUser() != null) {
            response.setUser(publicUser ? toPublicResponse(dto.getUser()) : toResponse(dto.getUser()));
        }
        if (dto.getReplyToUser() != null) {
            response.setReplyToUser(publicUser
                ? toPublicResponse(dto.getReplyToUser())
                : toResponse(dto.getReplyToUser()));
        }
        if (dto.getReplyPreview() != null && !dto.getReplyPreview().isEmpty()) {
            response.setReplyPreview(dto.getReplyPreview().stream()
                .map(reply -> publicUser ? toPublicResponse(reply) : toResponse(reply))
                .collect(java.util.stream.Collectors.toList()));
        }
        return response;
    }
}
