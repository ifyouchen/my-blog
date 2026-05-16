package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.repository.ConsecutiveSignInRewardRepository;
import com.myblog.growth.domain.repository.CumulativeSignInRewardRepository;
import com.myblog.growth.domain.repository.LevelRewardRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ConsecutiveSignInRewardMapper;
import com.myblog.growth.infrastructure.repository.persistence.mapper.CumulativeSignInRewardMapper;
import com.myblog.growth.infrastructure.repository.persistence.mapper.LevelRewardMapper;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：奖励配置接口.
 * <p>
 * 所有接口均需要 ADMIN 角色，否则返回 403。
 * </p>
 *
 * <pre>
 * 等级奖励：
 *   GET  /api/admin/rewards/level              — 查询所有等级奖励配置
 *   PUT  /api/admin/rewards/level/{id}         — 更新等级奖励配置
 *
 * 连续签到奖励：
 *   GET  /api/admin/rewards/sign-in/consecutive — 查询所有连续签到奖励配置
 *   PUT  /api/admin/rewards/sign-in/consecutive/{id} — 更新连续签到奖励配置
 *
 * 累计签到里程碑：
 *   GET  /api/admin/rewards/sign-in/cumulative  — 查询所有累计签到里程碑配置
 *   PUT  /api/admin/rewards/sign-in/cumulative/{id} — 更新累计签到里程碑配置
 * </pre>
 *
 * @author czx
 * @since 2026-05-17
 */
@RestController
@RequestMapping("/api/admin/rewards")
public class AdminRewardConfigController {

    private final LevelRewardRepository levelRewardRepository;
    private final ConsecutiveSignInRewardRepository consecutiveRewardRepository;
    private final CumulativeSignInRewardRepository cumulativeRewardRepository;
    private final LevelRewardMapper levelRewardMapper;
    private final ConsecutiveSignInRewardMapper consecutiveRewardMapper;
    private final CumulativeSignInRewardMapper cumulativeRewardMapper;

    public AdminRewardConfigController(LevelRewardRepository levelRewardRepository,
                                        ConsecutiveSignInRewardRepository consecutiveRewardRepository,
                                        CumulativeSignInRewardRepository cumulativeRewardRepository,
                                        LevelRewardMapper levelRewardMapper,
                                        ConsecutiveSignInRewardMapper consecutiveRewardMapper,
                                        CumulativeSignInRewardMapper cumulativeRewardMapper) {
        this.levelRewardRepository = levelRewardRepository;
        this.consecutiveRewardRepository = consecutiveRewardRepository;
        this.cumulativeRewardRepository = cumulativeRewardRepository;
        this.levelRewardMapper = levelRewardMapper;
        this.consecutiveRewardMapper = consecutiveRewardMapper;
        this.cumulativeRewardMapper = cumulativeRewardMapper;
    }

    /**
     * 检查管理员权限.
     */
    private void requireAdmin() {
        String role = AuthContext.getRole();
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }

    // ────────────────────────── 等级奖励 ──────────────────────────────

    @GetMapping("/level")
    public Result<List<LevelRewardConfig>> getLevelRewards() {
        requireAdmin();
        return Result.success(levelRewardRepository.findAllEnabled());
    }

    @PutMapping("/level/{id}")
    public Result<Void> updateLevelReward(@PathVariable Long id,
                                           @RequestBody LevelRewardConfig request) {
        requireAdmin();
        request.setId(id);
        levelRewardMapper.updateById(request);
        return Result.success(null);
    }

    // ────────────────────────── 连续签到奖励 ──────────────────────────────

    @GetMapping("/sign-in/consecutive")
    public Result<List<ConsecutiveSignInRewardConfig>> getConsecutiveRewards() {
        requireAdmin();
        return Result.success(consecutiveRewardRepository.findAllEnabled());
    }

    @PutMapping("/sign-in/consecutive/{id}")
    public Result<Void> updateConsecutiveReward(@PathVariable Long id,
                                                  @RequestBody ConsecutiveSignInRewardConfig request) {
        requireAdmin();
        request.setId(id);
        consecutiveRewardMapper.updateById(request);
        return Result.success(null);
    }

    // ────────────────────────── 累计签到里程碑 ──────────────────────────────

    @GetMapping("/sign-in/cumulative")
    public Result<List<CumulativeSignInRewardConfig>> getCumulativeRewards() {
        requireAdmin();
        return Result.success(cumulativeRewardRepository.findAllEnabled());
    }

    @PutMapping("/sign-in/cumulative/{id}")
    public Result<Void> updateCumulativeReward(@PathVariable Long id,
                                                 @RequestBody CumulativeSignInRewardConfig request) {
        requireAdmin();
        request.setId(id);
        cumulativeRewardMapper.updateById(request);
        return Result.success(null);
    }
}
