package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.AdminPointAppService;
import com.myblog.growth.application.service.PointAppService;
import com.myblog.growth.domain.model.aggregate.PointAccount;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.interfaces.rest.dto.request.AdminPointAdjustRequest;
import com.myblog.growth.interfaces.rest.dto.response.AdminAdjustResultVO;
import com.myblog.growth.interfaces.rest.dto.response.PointAccountVO;
import com.myblog.growth.interfaces.rest.dto.response.PointJournalVO;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员积分管理接口.
 *
 * <pre>
 * GET  /api/admin/points/account — 查询指定用户积分账户（需 ADMIN 角色）
 * GET  /api/admin/points/journals — 查询指定用户积分流水（需 ADMIN 角色）
 * POST /api/admin/points/adjust  — 管理员调整用户积分（需 ADMIN 角色）
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/points")
public class AdminPointController {

    private final AdminPointAppService adminPointAppService;
    private final PointAppService pointAppService;

    /**
     * 构造注入.
     *
     * @param adminPointAppService 管理员积分调整应用服务
     * @param pointAppService      积分查询应用服务
     */
    public AdminPointController(AdminPointAppService adminPointAppService,
                                PointAppService pointAppService) {
        this.adminPointAppService = adminPointAppService;
        this.pointAppService = pointAppService;
    }

    /**
     * 查询指定用户积分账户（管理员视角）.
     *
     * @param userId 目标用户 ID
     * @return 积分账户 VO
     */
    @GetMapping("/account")
    public Result<PointAccountVO> getAccount(@RequestParam Long userId) {
        requireAdmin();
        if (userId == null || userId <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "userId 不合法");
        }
        PointAccount account = adminPointAppService.getAccountByUserId(userId);
        PointAccountVO vo = new PointAccountVO();
        vo.setUserId(account.getUserId());
        vo.setBalance(account.getBalance());
        vo.setTotalEarned(account.getTotalEarned());
        vo.setTotalSpent(account.getTotalSpent());
        return Result.success(vo);
    }

    /**
     * 查询指定用户积分流水（管理员视角）.
     *
     * @param userId     目标用户 ID
     * @param sourceType 来源类型筛选（可选）
     * @param page       页码（从 1 开始，默认 1）
     * @param size       每页条数（默认 20，最大 50）
     * @return 积分流水分页结果
     */
    @GetMapping("/journals")
    public Result<PageResult<PointJournalVO>> getJournals(
            @RequestParam Long userId,
            @RequestParam(required = false) String sourceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();
        if (userId == null || userId <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "userId 不合法");
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        long total = pointAppService.countJournals(userId, sourceType);
        List<PointJournal> journals = pointAppService.getJournals(userId, sourceType, safePage, safeSize);
        List<PointJournalVO> voList = journals.stream()
                .map(this::toJournalVO)
                .collect(Collectors.toList());
        return Result.success(new PageResult<>(voList, safePage, safeSize, total));
    }

    /**
     * 管理员调整用户积分.
     *
     * @param request 调分请求体（targetUserId、delta、reason、bizNo 均必填）
     * @return 调整结果（targetUserId + balanceAfter）
     */
    @PostMapping("/adjust")
    public Result<AdminAdjustResultVO> adjustPoints(@RequestBody AdminPointAdjustRequest request) {
        requireAdmin();
        // 参数校验
        if (request.getTargetUserId() == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "targetUserId 不能为空");
        }
        if (request.getDelta() == null || request.getDelta() == 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "delta 不能为空且不能为 0");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "reason 不能为空");
        }
        if (request.getBizNo() == null || request.getBizNo().trim().isEmpty()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "bizNo 不能为空");
        }

        Long operatorUserId = AuthContext.getRequiredUserId();
        String operatorUsername = AuthContext.getUsername();

        int balanceAfter = adminPointAppService.adjustPoints(
                request.getTargetUserId(),
                request.getDelta(),
                request.getReason(),
                request.getBizNo(),
                operatorUserId,
                operatorUsername
        );

        return Result.success(new AdminAdjustResultVO(request.getTargetUserId(), balanceAfter));
    }

    private PointJournalVO toJournalVO(PointJournal journal) {
        PointJournalVO vo = new PointJournalVO();
        vo.setId(journal.getId());
        vo.setDelta(journal.getDelta());
        vo.setBalanceAfter(journal.getBalanceAfter());
        vo.setSourceType(journal.getSourceType());
        vo.setBizNo(journal.getBizNo());
        vo.setRemark(journal.getRemark());
        vo.setCreatedAt(journal.getCreatedAt());
        return vo;
    }

    private void requireAdmin() {
        String role = AuthContext.getRole();
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }
}

