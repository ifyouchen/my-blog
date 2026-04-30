package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.AdCampaignDTO;
import com.myblog.application.dto.AdStatsDTO;
import com.myblog.application.service.AdAppService;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 后台广告管理接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/ads")
public class AdminAdsController {

    private final AdAppService adAppService;
    private final AdminLogAppService adminLogAppService;

    public AdminAdsController(AdAppService adAppService, AdminLogAppService adminLogAppService) {
        this.adAppService = adAppService;
        this.adminLogAppService = adminLogAppService;
    }

    /**
     * 后台广告统计概览。
     */
    @GetMapping("/stats")
    public Result<AdStatsDTO> getStats() {
        ensureAdmin();
        return Result.success(adAppService.getStats());
    }

    /**
     * 后台分页查询广告列表。
     */
    @GetMapping
    public Result<PageResult<AdCampaignDTO>> pageAds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String slotCode,
            @RequestParam(required = false) Boolean enabled) {
        ensureAdmin();
        return Result.success(adAppService.listForAdmin(page, pageSize, slotCode, enabled));
    }

    /**
     * 后台获取广告详情。
     */
    @GetMapping("/{id}")
    public Result<AdCampaignDTO> getAd(@PathVariable Long id) {
        ensureAdmin();
        return Result.success(adAppService.getForAdmin(id));
    }

    /**
     * 后台创建广告。
     */
    @PostMapping
    public Result<AdCampaignDTO> createAd(
            @RequestParam String slotCode,
            @RequestParam String title,
            @RequestParam(required = false) String imageUrl,
            @RequestParam String targetUrl,
            @RequestParam(required = false, defaultValue = "广告") String label,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endAt,
            @RequestParam(required = false, defaultValue = "true") Boolean enabled,
            @RequestParam(required = false, defaultValue = "0") Integer sortOrder,
            @Nullable HttpServletRequest request) {
        ensureAdmin();
        AdCampaignDTO dto = adAppService.create(slotCode, title, imageUrl, targetUrl, label, startAt, endAt, enabled, sortOrder);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_AD", "AD_CAMPAIGN", dto.getId(), "创建广告: " + title, null, dto, request
        ));
        return Result.success(dto);
    }

    /**
     * 后台更新广告。
     */
    @PutMapping("/{id}")
    public Result<AdCampaignDTO> updateAd(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String imageUrl,
            @RequestParam String targetUrl,
            @RequestParam(required = false, defaultValue = "广告") String label,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endAt,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer sortOrder,
            @Nullable HttpServletRequest request) {
        ensureAdmin();
        AdCampaignDTO before = adAppService.getForAdmin(id);
        AdCampaignDTO after = adAppService.update(id, title, imageUrl, targetUrl, label, startAt, endAt, enabled, sortOrder);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_AD", "AD_CAMPAIGN", id, "更新广告 #" + id, before, after, request
        ));
        return Result.success(after);
    }

    /**
     * 后台删除广告（软删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAd(@PathVariable Long id, @Nullable HttpServletRequest request) {
        ensureAdmin();
        AdCampaignDTO before = adAppService.getForAdmin(id);
        adAppService.delete(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_AD", "AD_CAMPAIGN", id, "删除广告 #" + id, before, null, request
        ));
        return Result.success(null);
    }

    private void ensureAdmin() {
        AuthContext.getRequiredUserId();
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "仅管理员可访问后台");
        }
    }

    private RecordAdminLogCommand buildLogCommand(String operation, String targetType, Long targetId,
                                                   String detail, Object before, Object after,
                                                   @Nullable HttpServletRequest request) {
        RecordAdminLogCommand command = new RecordAdminLogCommand();
        command.setAdminUserId(AuthContext.getRequiredUserId());
        command.setAdminUsername(AuthContext.getUsername());
        command.setOperation(operation);
        command.setTargetType(targetType);
        command.setTargetId(targetId);
        command.setDetail(detail);
        command.setRequestMethod(request == null ? null : request.getMethod());
        command.setRequestUri(request == null ? null : request.getRequestURI());
        command.setIpAddress(resolveIp(request));
        command.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        command.setResultStatus("SUCCESS");
        command.setBeforeSnapshot(before);
        command.setAfterSnapshot(after);
        return command;
    }

    private String resolveIp(@Nullable HttpServletRequest request) {
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

