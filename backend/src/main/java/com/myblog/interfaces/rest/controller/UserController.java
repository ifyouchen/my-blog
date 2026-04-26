package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.MyArticleOverviewDTO;
import com.myblog.application.dto.UserProfileDTO;
import com.myblog.application.service.FollowAppService;
import com.myblog.application.service.UserAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.UpdateProfileRequest;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.UserResponse;
import com.myblog.interfaces.rest.dto.response.UserProfileResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAppService userAppService;
    private final FollowAppService followAppService;
    private final RestDtoMapper restDtoMapper;

    /**
     * 创建用户 REST 接口。
     *
     * @param userAppService 用户应用服务
     * @param restDtoMapper DTO 转换器
     */
    public UserController(UserAppService userAppService,
                          FollowAppService followAppService,
                          RestDtoMapper restDtoMapper) {
        this.userAppService = userAppService;
        this.followAppService = followAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 获取用户主页。
     *
     * @param id 用户 ID
     * @return 用户主页
     */
    @GetMapping("/{id}")
    public Result<UserProfileResponse> getUserProfile(@PathVariable Long id) {
        return Result.success(restDtoMapper.toResponse(userAppService.getUserProfile(id, AuthContext.getCurrentUserId())));
    }

    /**
     * 获取用户已发布文章。
     *
     * @param id 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 文章分页
     */
    @GetMapping("/{id}/articles")
    public Result<PageResult<ArticleResponse>> getUserArticles(@PathVariable Long id,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(toArticlePage(userAppService.pagePublishedArticles(id, page, pageSize)));
    }

    /**
     * 获取用户热门文章。
     *
     * @param id 用户 ID
     * @param limit 限制数量
     * @return 热门文章列表
     */
    @GetMapping("/{id}/articles/hot")
    public Result<List<ArticleResponse>> getUserHotArticles(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "3") int limit) {
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
        for (ArticleDTO articleDTO : userAppService.listHotPublishedArticles(id, limit)) {
            items.add(restDtoMapper.toResponse(articleDTO));
        }
        return Result.success(items);
    }

    /**
     * 获取当前用户文章。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 状态
     * @return 文章分页
     */
    @GetMapping("/me/articles")
    public Result<PageResult<ArticleResponse>> getMyArticles(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int pageSize,
                                                             @RequestParam(required = false) String status) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(toArticlePage(userAppService.pageMyArticles(userId, page, pageSize, status)));
    }

    /**
     * 获取当前用户创作概览。
     *
     * @return 创作概览
     */
    @GetMapping("/me/articles/overview")
    public Result<MyArticleOverviewDTO> getMyArticleOverview() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(userAppService.getMyArticleOverview(userId));
    }

    /**
     * 关注作者。
     *
     * @param id 作者 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/follow")
    public Result<Void> followUser(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        followAppService.followUser(id, userId);
        return Result.success();
    }

    /**
     * 取消关注作者。
     *
     * @param id 作者 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollowUser(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        followAppService.unfollowUser(id, userId);
        return Result.success();
    }

    /**
     * 获取当前用户关注作者。
     *
     * @return 作者列表
     */
    @GetMapping("/me/following")
    public Result<List<UserResponse>> getMyFollowingUsers() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        List<UserResponse> items = new ArrayList<UserResponse>();
        for (com.myblog.application.dto.UserDTO userDTO : followAppService.listFollowingUsers(userId)) {
            items.add(restDtoMapper.toResponse(userDTO));
        }
        return Result.success(items);
    }

    /**
     * 获取当前用户关注流。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param sort 排序方式
     * @return 关注流
     */
    @GetMapping("/me/feed")
    public Result<PageResult<ArticleResponse>> getMyFollowingFeed(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                                  @RequestParam(defaultValue = "latest") String sort) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(toArticlePage(followAppService.pageFollowingFeed(userId, page, pageSize, sort)));
    }

    /**
     * 更新当前用户个人资料。
     *
     * @param request 更新请求
     * @return 用户信息
     */
    @PutMapping("/me/profile")
    public Result<UserResponse> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(restDtoMapper.toResponse(userAppService.updateProfile(
            userId,
            request.getNickname(),
            request.getAvatarUrl(),
            request.getBio()
        )));
    }

    private PageResult<ArticleResponse> toArticlePage(PageResult<ArticleDTO> pageResult) {
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
        for (ArticleDTO articleDTO : pageResult.getItems()) {
            items.add(restDtoMapper.toResponse(articleDTO));
        }
        return new PageResult<ArticleResponse>(items, pageResult.getPage(), pageResult.getPageSize(), pageResult.getTotal());
    }
}
