package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.InviteCodeAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 邀请码 REST 控制器。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/invite-codes")
public class InviteCodeController {

    private final InviteCodeAppService inviteCodeAppService;

    public InviteCodeController(InviteCodeAppService inviteCodeAppService) {
        this.inviteCodeAppService = inviteCodeAppService;
    }

    /**
     * 当前用户获取或创建邀请码。
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate() {
        Long userId = AuthContext.getRequiredUserId();
        return Result.success(inviteCodeAppService.generate(userId));
    }

    /**
     * 当前用户重新生成邀请码（旧码失效）。
     */
    @PostMapping("/regenerate")
    public Result<Map<String, Object>> regenerate() {
        Long userId = AuthContext.getRequiredUserId();
        return Result.success(inviteCodeAppService.regenerate(userId));
    }

    /**
     * 获取当前用户的邀请码信息。
     */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myList() {
        Long userId = AuthContext.getRequiredUserId();
        return Result.success(inviteCodeAppService.listByUser(userId));
    }

    /**
     * 管理员分页查询邀请码（ADMIN 角色）。
     */
    @GetMapping("/admin")
    public Result<PageResult<Map<String, Object>>> adminPage(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        String role = AuthContext.getRole();
        if (!"ADMIN".equals(role)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权限");
        }
        return Result.success(inviteCodeAppService.adminPage(keyword, page, pageSize));
    }

    /**
     * 管理员删除邀请码（ADMIN 角色）。
     */
    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        String role = AuthContext.getRole();
        if (!"ADMIN".equals(role)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权限");
        }
        inviteCodeAppService.adminDelete(id);
        return Result.success(null);
    }
}

