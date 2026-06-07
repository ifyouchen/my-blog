package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.infrastructure.repository.persistence.repository.InviteRelationRepositoryImpl;
import com.myblog.growth.interfaces.rest.dto.response.AdminInviteRecordVO;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端邀请记录查询接口.
 *
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/admin/invite")
public class AdminInviteController {

    private final InviteRelationRepositoryImpl inviteRelationRepository;

    public AdminInviteController(InviteRelationRepositoryImpl inviteRelationRepository) {
        this.inviteRelationRepository = inviteRelationRepository;
    }

    /**
     * 分页查询邀请记录.
     *
     * @param keyword      搜索关键词（邀请人/被邀请人用户名或邀请码）
     * @param rewardStatus 奖励状态筛选（PENDING/GRANTED/SKIPPED）
     * @param page         页码
     * @param size         每页数量
     * @return 分页邀请记录
     */
    @GetMapping("/records")
    public Result<PageResult<AdminInviteRecordVO>> getRecords(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String rewardStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();

        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        String normalizedKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        String normalizedStatus = (rewardStatus == null || rewardStatus.trim().isEmpty()) ? null : rewardStatus.trim();
        int offset = (safePage - 1) * safeSize;

        long total = inviteRelationRepository.countForAdmin(normalizedKeyword, normalizedStatus);
        List<Map<String, Object>> rows = inviteRelationRepository.findPageForAdmin(
                normalizedKeyword, normalizedStatus, offset, safeSize);

        List<AdminInviteRecordVO> items = rows.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(new PageResult<>(items, safePage, safeSize, total));
    }

    private AdminInviteRecordVO toVO(Map<String, Object> row) {
        AdminInviteRecordVO vo = new AdminInviteRecordVO();
        vo.setId(toLong(row.get("id")));
        vo.setInviterUserId(toLong(row.get("inviterUserId")));
        vo.setInviterUsername((String) row.get("inviterUsername"));
        vo.setInviteeUserId(toLong(row.get("inviteeUserId")));
        vo.setInviteeUsername((String) row.get("inviteeUsername"));
        vo.setInviteCode((String) row.get("inviteCode"));
        vo.setRewardStatus((String) row.get("rewardStatus"));
        vo.setRewardGrantedAt((LocalDateTime) row.get("rewardGrantedAt"));
        vo.setSkipReason((String) row.get("skipReason"));
        vo.setTriggerBizNo((String) row.get("triggerBizNo"));
        vo.setCreatedAt((LocalDateTime) row.get("createdAt"));
        return vo;
    }

    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private void requireAdmin() {
        if (!UserRole.ADMIN.name().equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }
}
