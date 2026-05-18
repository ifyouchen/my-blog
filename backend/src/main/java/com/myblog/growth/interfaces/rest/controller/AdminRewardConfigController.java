package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.RewardConfigAppService;
import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.interfaces.rest.dto.response.AdminRewardGrantLogVO;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端奖励配置与奖励领取记录接口.
 *
 * @author Codex
 * @since 2026-05-17
 */
@RestController
@RequestMapping("/api/admin/rewards")
public class AdminRewardConfigController {

    private final RewardConfigAppService rewardConfigAppService;

    /**
     * 构造注入.
     *
     * @param rewardConfigAppService 奖励配置应用服务
     */
    public AdminRewardConfigController(RewardConfigAppService rewardConfigAppService) {
        this.rewardConfigAppService = rewardConfigAppService;
    }

    /**
     * 查询全部等级奖励配置.
     *
     * @return 配置列表
     */
    @GetMapping("/level")
    public Result<List<LevelRewardConfig>> getLevelRewards() {
        requireAdmin();
        return Result.success(rewardConfigAppService.listLevelRewards());
    }

    /**
     * 新增等级奖励配置.
     *
     * @param request 配置内容
     * @return 配置ID
     */
    @PostMapping("/level")
    public Result<Long> createLevelReward(@RequestBody LevelRewardConfig request) {
        requireAdmin();
        return Result.success(rewardConfigAppService.createLevelReward(request));
    }

    /**
     * 更新等级奖励配置.
     *
     * @param id      配置ID
     * @param request 配置内容
     * @return 空结果
     */
    @PutMapping("/level/{id}")
    public Result<Void> updateLevelReward(@PathVariable Long id, @RequestBody LevelRewardConfig request) {
        requireAdmin();
        request.setId(id);
        rewardConfigAppService.updateLevelReward(request);
        return Result.success();
    }

    /**
     * 删除等级奖励配置.
     *
     * @param id      配置ID
     * @param version 当前版本
     * @return 空结果
     */
    @DeleteMapping("/level/{id}")
    public Result<Void> deleteLevelReward(@PathVariable Long id, @RequestParam int version) {
        requireAdmin();
        rewardConfigAppService.deleteLevelReward(id, version);
        return Result.success();
    }

    /**
     * 查询全部连续签到奖励配置.
     *
     * @return 配置列表
     */
    @GetMapping("/sign-in/consecutive")
    public Result<List<ConsecutiveSignInRewardConfig>> getConsecutiveRewards() {
        requireAdmin();
        return Result.success(rewardConfigAppService.listConsecutiveRewards());
    }

    /**
     * 新增连续签到奖励配置.
     *
     * @param request 配置内容
     * @return 配置ID
     */
    @PostMapping("/sign-in/consecutive")
    public Result<Long> createConsecutiveReward(@RequestBody ConsecutiveSignInRewardConfig request) {
        requireAdmin();
        return Result.success(rewardConfigAppService.createConsecutiveReward(request));
    }

    /**
     * 更新连续签到奖励配置.
     *
     * @param id      配置ID
     * @param request 配置内容
     * @return 空结果
     */
    @PutMapping("/sign-in/consecutive/{id}")
    public Result<Void> updateConsecutiveReward(@PathVariable Long id,
                                                @RequestBody ConsecutiveSignInRewardConfig request) {
        requireAdmin();
        request.setId(id);
        rewardConfigAppService.updateConsecutiveReward(request);
        return Result.success();
    }

    /**
     * 删除连续签到奖励配置.
     *
     * @param id      配置ID
     * @param version 当前版本
     * @return 空结果
     */
    @DeleteMapping("/sign-in/consecutive/{id}")
    public Result<Void> deleteConsecutiveReward(@PathVariable Long id, @RequestParam int version) {
        requireAdmin();
        rewardConfigAppService.deleteConsecutiveReward(id, version);
        return Result.success();
    }

    /**
     * 查询全部累计签到里程碑配置.
     *
     * @return 配置列表
     */
    @GetMapping("/sign-in/cumulative")
    public Result<List<CumulativeSignInRewardConfig>> getCumulativeRewards() {
        requireAdmin();
        return Result.success(rewardConfigAppService.listCumulativeRewards());
    }

    /**
     * 新增累计签到里程碑配置.
     *
     * @param request 配置内容
     * @return 配置ID
     */
    @PostMapping("/sign-in/cumulative")
    public Result<Long> createCumulativeReward(@RequestBody CumulativeSignInRewardConfig request) {
        requireAdmin();
        return Result.success(rewardConfigAppService.createCumulativeReward(request));
    }

    /**
     * 更新累计签到里程碑配置.
     *
     * @param id      配置ID
     * @param request 配置内容
     * @return 空结果
     */
    @PutMapping("/sign-in/cumulative/{id}")
    public Result<Void> updateCumulativeReward(@PathVariable Long id,
                                               @RequestBody CumulativeSignInRewardConfig request) {
        requireAdmin();
        request.setId(id);
        rewardConfigAppService.updateCumulativeReward(request);
        return Result.success();
    }

    /**
     * 删除累计签到里程碑配置.
     *
     * @param id      配置ID
     * @param version 当前版本
     * @return 空结果
     */
    @DeleteMapping("/sign-in/cumulative/{id}")
    public Result<Void> deleteCumulativeReward(@PathVariable Long id, @RequestParam int version) {
        requireAdmin();
        rewardConfigAppService.deleteCumulativeReward(id, version);
        return Result.success();
    }

    /**
     * 分页查询奖励领取记录.
     *
     * @param userId      用户ID（可选）
     * @param userKeyword 用户名或邮箱（可选）
     * @param rewardType  奖励类型（可选）
     * @param page        页码
     * @param size        每页数量
     * @return 分页记录
     */
    @GetMapping("/grant-logs")
    public Result<PageResult<AdminRewardGrantLogVO>> getGrantLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userKeyword,
            @RequestParam(required = false) String rewardType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();
        return Result.success(rewardConfigAppService.pageGrantLogs(userId, userKeyword, rewardType, page, size));
    }

    private void requireAdmin() {
        String role = AuthContext.getRole();
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }
}
