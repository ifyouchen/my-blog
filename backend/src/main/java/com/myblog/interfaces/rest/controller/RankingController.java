package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.service.RankingAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.AuthorRankingResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingAppService rankingAppService;
    private final RestDtoMapper restDtoMapper;

    public RankingController(RankingAppService rankingAppService, RestDtoMapper restDtoMapper) {
        this.rankingAppService = rankingAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 获取文章排行榜。
     *
     * @param limit    返回条数，默认 10
     * @param period   时间段（7d/30d/all）
     * @param category 分类名称，可为 null（不过滤）
     * @return 文章排行榜列表
     */
    @GetMapping("/articles")
    public Result<List<ArticleResponse>> listArticleRankings(@RequestParam(defaultValue = "10") int limit,
                                                             @RequestParam(defaultValue = "7d") String period,
                                                             @RequestParam(required = false) String category) {
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
        for (ArticleDTO item : rankingAppService.listArticleRankings(limit, period, category)) {
            items.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(items);
    }

    /**
     * 获取作者排行榜。
     *
     * @param limit    返回条数，默认 10
     * @param period   时间段（7d/30d/all）
     * @param category 分类名称，可为 null（不过滤）
     * @return 作者排行榜列表
     */
    @GetMapping("/authors")
    public Result<List<AuthorRankingResponse>> listAuthorRankings(@RequestParam(defaultValue = "10") int limit,
                                                                  @RequestParam(defaultValue = "7d") String period,
                                                                  @RequestParam(required = false) String category) {
        List<AuthorRankingResponse> items = new ArrayList<AuthorRankingResponse>();
        for (AuthorRankingDTO item : rankingAppService.listAuthorRankings(
                limit,
                period,
                category,
                AuthContext.getCurrentUserId())) {
            items.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(items);
    }
}
