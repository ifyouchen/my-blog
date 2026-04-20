package com.myblog.interfaces.rest.mapper;

import com.myblog.application.command.CreateArticleCommand;
import com.myblog.application.command.CreateCommentCommand;
import com.myblog.application.command.LoginCommand;
import com.myblog.application.command.RegisterCommand;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthDTO;
import com.myblog.application.dto.CommentDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.interfaces.rest.dto.request.CreateArticleRequest;
import com.myblog.interfaces.rest.dto.request.CreateCommentRequest;
import com.myblog.interfaces.rest.dto.request.LoginRequest;
import com.myblog.interfaces.rest.dto.request.RegisterRequest;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.AuthResponse;
import com.myblog.interfaces.rest.dto.response.CommentResponse;
import com.myblog.interfaces.rest.dto.response.UserResponse;
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
        return new RegisterCommand(request.getUsername(), request.getEmail(), request.getPassword());
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
     * 将用户 DTO 转换为响应。
     *
     * @param dto 用户 DTO
     * @return 用户响应
     */
    public UserResponse toResponse(UserDTO dto) {
        UserResponse response = new UserResponse();
        response.setId(dto.getId());
        response.setUsername(dto.getUsername());
        response.setEmail(dto.getEmail());
        response.setNickname(dto.getNickname());
        response.setAvatarUrl(dto.getAvatarUrl());
        response.setBio(dto.getBio());
        response.setRole(dto.getRole());
        return response;
    }

    /**
     * 将文章 DTO 转换为响应。
     *
     * @param dto 文章 DTO
     * @return 文章响应
     */
    public ArticleResponse toResponse(ArticleDTO dto) {
        ArticleResponse response = new ArticleResponse();
        response.setId(dto.getId());
        response.setTitle(dto.getTitle());
        response.setSummary(dto.getSummary());
        response.setContent(dto.getContent());
        response.setCoverUrl(dto.getCoverUrl());
        response.setCategory(dto.getCategory());
        response.setTags(dto.getTags());
        response.setStatus(dto.getStatus());
        response.setViewCount(dto.getViewCount());
        response.setLikeCount(dto.getLikeCount());
        response.setFavoriteCount(dto.getFavoriteCount());
        response.setCommentCount(dto.getCommentCount());
        response.setPublishedAt(dto.getPublishedAt());
        response.setAuthor(toResponse(dto.getAuthor()));
        return response;
    }

    /**
     * 将评论 DTO 转换为响应。
     *
     * @param dto 评论 DTO
     * @return 评论响应
     */
    public CommentResponse toResponse(CommentDTO dto) {
        CommentResponse response = new CommentResponse();
        response.setId(dto.getId());
        response.setArticleId(dto.getArticleId());
        response.setUserId(dto.getUserId());
        response.setParentId(dto.getParentId());
        response.setContent(dto.getContent());
        response.setCreatedAt(dto.getCreatedAt());
        if (dto.getUser() != null) {
            response.setUser(toResponse(dto.getUser()));
        }
        if (dto.getReplies() != null && !dto.getReplies().isEmpty()) {
            response.setReplies(dto.getReplies().stream()
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList()));
        }
        return response;
    }
}
