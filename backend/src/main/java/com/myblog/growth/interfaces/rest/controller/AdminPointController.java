package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.AdminPointAppService;
import com.myblog.growth.interfaces.rest.dto.request.AdminPointAdjustRequest;
import com.myblog.growth.interfaces.rest.dto.response.AdminAdjustResultVO;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员积分调整接口.
 *
 * <pre>
 * POST /api/admin/points/adjust — 管理员调整用户积分（需 ADMIN 角色）
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

