package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.MyArticleOverviewDTO;
import com.myblog.application.dto.UserProfileDTO;
import com.myblog.application.service.FollowAppService;
import com.myblog.application.service.UserAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.ChangePasswordRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        return Result.success(restDtoMapper.toPublicResponse(
            userAppService.getUserProfile(id, AuthContext.getCurrentUserId())
        ));
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
        return Result.success(toArticlePage(userAppService.pagePublishedArticles(id, page, pageSize), true));
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
            items.add(restDtoMapper.toPublicResponse(articleDTO));
        }
        return Result.success(items);
    }

    /**
     * 获取某用户的粉丝列表。
     *
     * @param id 目标用户 ID
     * @return 粉丝用户列表
     */
    @GetMapping("/{id}/followers")
    public Result<List<UserResponse>> getUserFollowers(@PathVariable Long id) {
        List<UserResponse> items = new ArrayList<UserResponse>();
        for (com.myblog.application.dto.UserDTO userDTO : followAppService.listFollowers(id)) {
            items.add(restDtoMapper.toPublicResponse(userDTO));
        }
        return Result.success(items);
    }

    /**
     * 获取某用户的关注列表。
     *
     * @param id 目标用户 ID
     * @return 关注用户列表
     */
    @GetMapping("/{id}/following")
    public Result<List<UserResponse>> getUserFollowing(@PathVariable Long id) {
        List<UserResponse> items = new ArrayList<UserResponse>();
        for (com.myblog.application.dto.UserDTO userDTO : followAppService.listUserFollowing(id)) {
            items.add(restDtoMapper.toPublicResponse(userDTO));
        }
        return Result.success(items);
    }

    /**
     * 获取当前用户对目标用户的关注状态（含互关）。
     *
     * @param id 目标用户 ID
     * @return 关注状态
     */
    @GetMapping("/{id}/follow-status")
    public Result<java.util.Map<String, Boolean>> getFollowStatus(@PathVariable Long id) {
        Long currentUserId = AuthContext.getCurrentUserId();
        return Result.success(followAppService.getFollowStatus(id, currentUserId));
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
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String keyword) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(toArticlePage(
            userAppService.pageMyArticles(userId, page, pageSize, status, keyword),
            false
        ));
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
                                                                  @RequestParam(defaultValue = "latest") String sort,
                                                                  @RequestParam(required = false) String category) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(toArticlePage(
            followAppService.pageFollowingFeed(userId, page, pageSize, sort, category),
            true
        ));
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
            request.getBio(),
            request.getWebsite(),
            request.getGithub(),
            request.getTwitter(),
            request.getLocation()
        )));
    }

    /**
     * 获取当前用户安全信息。
     *
     * @return 用户信息（含 lastLoginAt/lastLoginIp）
     */
    @GetMapping("/me/security")
    public Result<UserResponse> getSecurityInfo() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(restDtoMapper.toResponse(userAppService.getSecurityInfo(userId)));
    }

    /**
     * 修改当前用户密码。
     *
     * @param request 修改密码请求
     * @return 成功响应
     */
    @PostMapping("/me/password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        userAppService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return Result.success();
    }

    /**
     * 绑定/更换邮箱。
     *
     * @param request 请求体（email, password）
     * @return 更新后的用户信息
     */
    @PostMapping("/me/email")
    public Result<UserResponse> changeEmail(@RequestBody java.util.Map<String, String> request) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        String email = request.get("email");
        String password = request.get("password");
        if (email == null || email.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "邮箱不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "请输入密码以确认身份");
        }
        return Result.success(restDtoMapper.toResponse(
            userAppService.changeEmail(userId, email.trim(), password)
        ));
    }

    /**
     * 将文章 DTO 分页结果转换为响应对象分页结果。
     *
     * @param pageResult 文章 DTO 分页结果
     * @param publicUser 是否使用公开视图转换（公开为 true，自身为 false）
     * @return 文章响应分页结果
     */
    private PageResult<ArticleResponse> toArticlePage(PageResult<ArticleDTO> pageResult, boolean publicUser) {
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
        for (ArticleDTO articleDTO : pageResult.getItems()) {
            items.add(publicUser
                ? restDtoMapper.toPublicResponse(articleDTO)
                : restDtoMapper.toResponse(articleDTO));
        }
        return new PageResult<ArticleResponse>(
            items,
            pageResult.getPage(),
            pageResult.getPageSize(),
            pageResult.getTotal()
        );
    }

    /**
     * 导出当前用户的文章列表为 CSV 文件。
     *
     * @param status   文章状态过滤
     * @param keyword  关键字过滤
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    @GetMapping("/me/export/articles")
    public void exportMyArticles(@RequestParam(required = false) String status,
                                 @RequestParam(required = false) String keyword,
                                 HttpServletResponse response) throws IOException {
        Long userId = AuthContext.getRequiredUserId();
        String filename = "my-articles-"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        byte[] data = userAppService.exportMyArticlesCsv(userId, status, keyword);
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}
