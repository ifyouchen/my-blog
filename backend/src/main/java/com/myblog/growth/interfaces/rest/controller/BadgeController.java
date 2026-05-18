package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.dto.MyBadgesDTO;
import com.myblog.growth.application.service.BadgeAppService;
import com.myblog.growth.interfaces.rest.dto.request.EquipBadgeRequest;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 徽章接口.
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeAppService badgeAppService;

    public BadgeController(BadgeAppService badgeAppService) {
        this.badgeAppService = badgeAppService;
    }

    /**
     * 查询我的徽章.
     *
     * @return 我的徽章列表
     */
    @GetMapping("/my")
    public Result<MyBadgesDTO> getMyBadges() {
        return Result.success(badgeAppService.listMyBadges(AuthContext.getRequiredUserId()));
    }

    /**
     * 更新当前佩戴徽章.
     *
     * @param request 请求体
     * @return 更新后的我的徽章列表
     */
    @PutMapping("/my/equipped")
    public Result<MyBadgesDTO> updateEquippedBadge(@RequestBody(required = false) EquipBadgeRequest request) {
        String badgeCode = request == null ? null : request.getBadgeCode();
        return Result.success(badgeAppService.equipBadge(AuthContext.getRequiredUserId(), badgeCode));
    }
}
