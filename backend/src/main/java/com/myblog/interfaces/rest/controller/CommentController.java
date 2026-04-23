package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.CommentDTO;
import com.myblog.application.service.CommentAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.CreateCommentRequest;
import com.myblog.interfaces.rest.dto.response.CommentResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 创建评论控制器。
     *
     * @param commentAppService 评论应用服务
     * @param restDtoMapper REST DTO 转换器
     */
    public CommentController(CommentAppService commentAppService, RestDtoMapper restDtoMapper) {
        this.commentAppService = commentAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 分页查询文章一级评论。
     *
     * @param articleId 文章 ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param sort 排序方式
     * @return 评论分页结果
     */
    @GetMapping("/articles/{articleId}/comments")
    public Result<PageResult<CommentResponse>> pageArticleComments(@PathVariable Long articleId,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                                  @RequestParam(defaultValue = "hot") String sort) {
        PageResult<CommentDTO> result = commentAppService.pageArticleComments(
            articleId,
            page,
            pageSize,
            sort,
            AuthContext.getRequiredUserId(),
            AuthContext.getRole()
        );
        return Result.success(toPageResponse(result));
    }

    /**
     * 分页查询楼中楼回复。
     *
     * @param rootCommentId 根评论 ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 回复分页结果
     */
    @GetMapping("/comments/{rootCommentId}/replies")
    public Result<PageResult<CommentResponse>> pageReplies(@PathVariable Long rootCommentId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<CommentDTO> result = commentAppService.pageReplies(
            rootCommentId,
            page,
            pageSize,
            AuthContext.getRequiredUserId(),
            AuthContext.getRole()
        );
        return Result.success(toPageResponse(result));
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
            restDtoMapper.toCommand(request, articleId, userId),
            AuthContext.getRole()
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
        commentAppService.deleteComment(id, userId, AuthContext.getRole());
        return Result.success();
    }

    /**
     * 点赞评论。
     *
     * @param id 评论 ID
     * @return 点赞结果
     */
    @PostMapping("/comments/{id}/like")
    public Result<Map<String, Object>> likeComment(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        commentAppService.likeComment(id, userId);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("liked", true);
        return Result.success(result);
    }

    /**
     * 取消评论点赞。
     *
     * @param id 评论 ID
     * @return 点赞结果
     */
    @DeleteMapping("/comments/{id}/like")
    public Result<Map<String, Object>> unlikeComment(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        commentAppService.unlikeComment(id, userId);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("liked", false);
        return Result.success(result);
    }

    /**
     * 置顶评论。
     *
     * @param id 评论 ID
     * @return 置顶结果
     */
    @PostMapping("/comments/{id}/pin")
    public Result<Map<String, Object>> pinComment(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        commentAppService.pinComment(id, userId, AuthContext.getRole());
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("pinned", true);
        return Result.success(result);
    }

    /**
     * 取消置顶评论。
     *
     * @param id 评论 ID
     * @return 取消置顶结果
     */
    @DeleteMapping("/comments/{id}/pin")
    public Result<Map<String, Object>> unpinComment(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        commentAppService.unpinComment(id, userId, AuthContext.getRole());
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("pinned", false);
        return Result.success(result);
    }

    private PageResult<CommentResponse> toPageResponse(PageResult<CommentDTO> result) {
        List<CommentResponse> items = new ArrayList<CommentResponse>(result.getItems().size());
        for (CommentDTO item : result.getItems()) {
            items.add(restDtoMapper.toResponse(item));
        }
        return new PageResult<CommentResponse>(items, result.getPage(), result.getPageSize(), result.getTotal());
    }
}
