package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.SearchBootstrapDTO;
import com.myblog.application.dto.UserSearchDTO;
import com.myblog.application.service.ColumnAppService;
import com.myblog.application.service.SearchHistoryAppService;
import com.myblog.application.service.UserAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.response.ColumnResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final UserAppService userAppService;
    private final ColumnAppService columnAppService;
    private final SearchHistoryAppService searchHistoryAppService;
    private final RestDtoMapper restDtoMapper;

    public SearchController(UserAppService userAppService,
                            ColumnAppService columnAppService,
                            SearchHistoryAppService searchHistoryAppService,
                            RestDtoMapper restDtoMapper) {
        this.userAppService = userAppService;
        this.columnAppService = columnAppService;
        this.searchHistoryAppService = searchHistoryAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 搜索用户。
     *
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页数量
     * @return 用户分页结果
     */
    @GetMapping("/users")
    public Result<PageResult<UserSearchDTO>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long currentUserId = AuthContext.getCurrentUserId();
        PageResult<UserSearchDTO> result = userAppService.searchUsers(keyword, page, pageSize, currentUserId);
        return Result.success(result);
    }

    /**
     * 搜索专栏。
     *
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页数量
     * @return 专栏分页结果
     */
    @GetMapping("/columns")
    public Result<PageResult<ColumnResponse>> searchColumns(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long currentUserId = AuthContext.getCurrentUserId();
        PageResult<ColumnDTO> result = columnAppService.searchColumns(keyword, page, pageSize, currentUserId);
        List<ColumnResponse> items = new ArrayList<ColumnResponse>(result.getItems().size());
        for (ColumnDTO item : result.getItems()) {
            items.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(new PageResult<ColumnResponse>(
            items,
            result.getPage(),
            result.getPageSize(),
            result.getTotal()
        ));
    }

    /**
     * 获取搜索引导数据。
     *
     * @return 搜索引导数据（分类 + 标签 + 热门关键字 + 最近搜索）
     */
    @GetMapping("/bootstrap")
    public Result<SearchBootstrapDTO> getSearchBootstrap() {
        Long currentUserId = AuthContext.getCurrentUserId();
        SearchBootstrapDTO bootstrap = searchHistoryAppService.getSearchBootstrap(currentUserId);
        return Result.success(bootstrap);
    }

    /**
     * 获取热门搜索关键字。
     *
     * @param limit 限制数量
     * @return 热门关键字列表
     */
    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotKeywords(@RequestParam(defaultValue = "10") int limit) {
        List<String> hotKeywords = searchHistoryAppService.getHotKeywords(limit);
        return Result.success(hotKeywords);
    }

    /**
     * 获取最近搜索关键字。
     *
     * @param limit 限制数量
     * @return 最近关键字列表
     */
    @GetMapping("/recent-keywords")
    public Result<List<String>> getRecentKeywords(@RequestParam(defaultValue = "10") int limit) {
        Long currentUserId = AuthContext.getRequiredUserId();
        List<String> recentKeywords = searchHistoryAppService.getRecentKeywords(currentUserId, limit);
        return Result.success(recentKeywords);
    }

    /**
     * 记录搜索关键字。
     *
     * @param body 请求体，包含 keyword
     * @return 操作结果
     */
    @PostMapping("/recent-keywords")
    public Result<Void> recordSearch(@RequestBody Map<String, String> body) {
        Long currentUserId = AuthContext.getRequiredUserId();
        if (currentUserId == null) {
            return Result.success(null);
        }
        String keyword = body.get("keyword");
        searchHistoryAppService.recordSearch(currentUserId, keyword);
        return Result.success(null);
    }

    /**
     * 清空搜索历史。
     *
     * @return 操作结果
     */
    @DeleteMapping("/recent-keywords")
    public Result<Void> clearSearchHistory() {
        Long currentUserId = AuthContext.getRequiredUserId();
        if (currentUserId == null) {
            return Result.success(null);
        }
        searchHistoryAppService.clearUserSearchHistory(currentUserId);
        return Result.success(null);
    }
}
