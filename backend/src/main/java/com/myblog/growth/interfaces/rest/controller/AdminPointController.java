package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.AdminPointAppService;
import com.myblog.growth.domain.model.aggregate.PointAccount;
import com.myblog.growth.interfaces.rest.dto.request.AdminPointAdjustRequest;
import com.myblog.growth.interfaces.rest.dto.response.AdminAdjustResultVO;
import com.myblog.growth.interfaces.rest.dto.response.PointAccountVO;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员积分管理接口.
 *
 * <pre>
 * GET  /api/admin/points/account — 查询指定用户积分账户（需 ADMIN 角色）
 * POST /api/admin/points/adjust  — 管理员调整用户积分（需 ADMIN 角色）
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/points")
public class AdminPointController {

    private final AdminPointAppService adminPointAppService;

    /**
     * 构造注入.
     *
     * @param adminPointAppService 管理员积分调整应用服务
     */
    public AdminPointController(AdminPointAppService adminPointAppService) {
        this.adminPointAppService = adminPointAppService;
    }

    /**
     * 查询指定用户积分账户（管理员视角）.
     *
     * @param userId 目标用户 ID
     * @return 积分账户 VO
     */
    @GetMapping("/account")
    public Result<PointAccountVO> getAccount(@RequestParam Long userId) {
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
     * 管理员调整用户积分.
     *
     * @param request 调分请求体（targetUserId、delta、reason、bizNo 均必填）
     * @return 调整结果（targetUserId + balanceAfter）
     */
    @PostMapping("/adjust")
    public Result<AdminAdjustResultVO> adjustPoints(@RequestBody AdminPointAdjustRequest request) {
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
}

