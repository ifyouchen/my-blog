package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.GrowthQueryAppService;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import com.myblog.growth.domain.repository.LevelPrivilegeRepository;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.interfaces.rest.assembler.GrowthAssembler;
import com.myblog.growth.interfaces.rest.dto.response.ExpJournalVO;
import com.myblog.growth.interfaces.rest.dto.response.GrowthAccountVO;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户成长信息接口.
 * <p>
 * 提供当前登录用户的等级信息和经验流水查询接口，均需登录。
 * </p>
 *
 * <pre>
 * GET /api/growth/my          — 查询我的成长账户信息（等级、经验、进度）
 * GET /api/growth/my/journals — 查询我的经验流水列表
 * </pre>
 */
@RestController
@RequestMapping("/api/growth")
public class GrowthController {

    private final GrowthQueryAppService growthQueryAppService;
    private final LevelThresholdRepository levelThresholdRepository;
    private final LevelPrivilegeRepository levelPrivilegeRepository;
    private final GrowthAssembler growthAssembler;

    /**
     * 构造注入.
     *
     * @param growthQueryAppService    成长信息查询应用服务
     * @param levelThresholdRepository 等级阈值 Repository（供 Assembler 组装进度）
     * @param growthAssembler          成长模块 Assembler
     */
    public GrowthController(GrowthQueryAppService growthQueryAppService,
                             LevelThresholdRepository levelThresholdRepository,
                             LevelPrivilegeRepository levelPrivilegeRepository,
                             GrowthAssembler growthAssembler) {
        this.growthQueryAppService = growthQueryAppService;
        this.levelThresholdRepository = levelThresholdRepository;
        this.levelPrivilegeRepository = levelPrivilegeRepository;
        this.growthAssembler = growthAssembler;
    }

    /**
     * 查询当前用户的成长账户信息.
     * <p>包含：等级、经验值、等级名称、升级进度、距下一级所需经验。</p>
     *
     * @return 成长账户 VO
     */
    @GetMapping("/my")
    public Result<GrowthAccountVO> getMyGrowthAccount() {
        Long userId = AuthContext.getRequiredUserId();
        GrowthAccount account = growthQueryAppService.getGrowthAccount(userId);
        List<LevelThreshold> thresholds = levelThresholdRepository.findAllEnabled();
        List<LevelRewardConfig> levelRewards = growthQueryAppService.getVisibleLevelRewards();
        List<LevelPrivilegeConfig> privilegeConfigs = levelPrivilegeRepository.findAllEnabled();
        List<RewardGrantLog> grantLogs = growthQueryAppService.getRewardGrantLogs(userId);
        List<UserPrivilegeEntitlement> entitlements = growthQueryAppService.getActiveEntitlements(userId);
        GrowthAccountVO vo = growthAssembler.toVO(
            account,
            thresholds,
            levelRewards,
            privilegeConfigs,
            grantLogs,
            entitlements,
            growthQueryAppService.hasRegisterBonus(userId)
        );
        return Result.success(vo);
    }

    /**
     * 查询当前用户的经验流水列表.
     *
     * @param limit 最多返回条数（默认 20，最大 50）
     * @return 经验流水 VO 列表
     */
    @GetMapping("/my/journals")
    public Result<List<ExpJournalVO>> getMyJournals(
            @RequestParam(defaultValue = "20") int limit) {
        // 防止 limit 超限
        int safeLimit = Math.min(Math.max(1, limit), 50);
        Long userId = AuthContext.getRequiredUserId();
        List<ExpJournal> journals = growthQueryAppService.getRecentJournals(userId, safeLimit);
        return Result.success(growthAssembler.toJournalVOList(journals));
    }
}

