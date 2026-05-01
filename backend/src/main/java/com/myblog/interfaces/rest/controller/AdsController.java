package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.AdCampaignDTO;
import com.myblog.application.service.AdAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 广告前台接口。
 * <p>不需要登录即可访问，但点击/曝光事件会记录当前用户 ID（如果已登录）。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ads")
public class AdsController {

    private final AdAppService adAppService;

    public AdsController(AdAppService adAppService) {
        this.adAppService = adAppService;
    }

    /**
     * 查询广告位当前生效的广告列表。
     */
    @GetMapping
    public Result<List<AdCampaignDTO>> listAds(@RequestParam String slot) {
        return Result.success(adAppService.listActiveBySlot(slot));
    }

    /**
     * 记录广告曝光。
     */
    @PostMapping("/{campaignId}/impression")
    public Result<Void> impression(@PathVariable Long campaignId,
                                    @Nullable HttpServletRequest request) {
        Long userId = safeGetUserId();
        String ip = resolveIpAddress(request);
        String ua = request == null ? null : request.getHeader("User-Agent");
        adAppService.recordImpression(campaignId, userId, ip, ua);
        return Result.success(null);
    }

    /**
     * 记录广告点击。
     */
    @PostMapping("/{campaignId}/click")
    public Result<Void> click(@PathVariable Long campaignId,
                               @Nullable HttpServletRequest request) {
        Long userId = safeGetUserId();
        String ip = resolveIpAddress(request);
        String ua = request == null ? null : request.getHeader("User-Agent");
        adAppService.recordClick(campaignId, userId, ip, ua);
        return Result.success(null);
    }

    /**
     * 关闭广告（用户维度，3天有效）。
     */
    @PostMapping("/{campaignId}/dismiss")
    public Result<Void> dismiss(@PathVariable Long campaignId) {
        Long userId = safeGetUserId();
        if (userId != null) {
            adAppService.dismiss(campaignId, userId);
        }
        return Result.success(null);
    }

    /**
     * 获取当前用户 3 天内关闭过的广告 ID 列表。
     */
    @GetMapping("/dismissed-ids")
    public Result<Map<String, List<Long>>> getDismissedIds() {
        Long userId = safeGetUserId();
        List<Long> ids = adAppService.getUserDismissedIds(userId);
        Map<String, List<Long>> result = new HashMap<>();
        result.put("ids", ids);
        return Result.success(result);
    }

    private Long safeGetUserId() {
        try {
            return AuthContext.getRequiredUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveIpAddress(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

