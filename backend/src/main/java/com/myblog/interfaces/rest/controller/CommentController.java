package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.CommentDTO;
import com.myblog.application.service.CommentAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.CreateCommentRequest;
import com.myblog.interfaces.rest.dto.response.CommentResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentAppService commentAppService;
    private final RestDtoMapper restDtoMapper;

    /**
     * 创建评论接口。
     *
     * @param commentAppService 评论应用服务
     * @param restDtoMapper REST DTO 转换器
     */
    public CommentController(CommentAppService commentAppService, RestDtoMapper restDtoMapper) {
        this.commentAppService = commentAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 获取文章评论列表。
     *
     * @param articleId 文章 ID
     * @return 评论列表响应
     */
    @GetMapping("/articles/{articleId}/comments")
    public Result<List<CommentResponse>> getArticleComments(@PathVariable Long articleId) {
        List<CommentDTO> comments = commentAppService.getArticleComments(articleId);
        List<CommentResponse> responses = comments.stream()
            .map(restDtoMapper::toResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 创建评论或回复。
     *
     * @param articleId 文章 ID
     * @param request 创建评论请求
     * @return 评论响应
     */
    @PostMapping("/articles/{articleId}/comments")
    public Result<CommentResponse> createComment(@PathVariable Long articleId,
                                                 @RequestBody @Valid CreateCommentRequest request) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        CommentDTO comment = commentAppService.createComment(
            restDtoMapper.toCommand(request, articleId, userId)
        );
        return Result.success(restDtoMapper.toResponse(comment));
    }

    /**
     * 删除评论。
     *
     * @param id 评论 ID
     * @return 成功响应
     */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        commentAppService.deleteComment(id, userId);
        return Result.success();
    }
}
