package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.GrowthRuleAppService;
import com.myblog.growth.application.service.PointRuleAppService;
import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.interfaces.rest.assembler.GrowthAssembler;
import com.myblog.growth.interfaces.rest.dto.request.BatchSaveThresholdRequest;
import com.myblog.growth.interfaces.rest.dto.request.SavePointRuleRequest;
import com.myblog.growth.interfaces.rest.dto.request.SaveRuleRequest;
import com.myblog.growth.interfaces.rest.dto.response.GrowthRuleVO;
import com.myblog.growth.interfaces.rest.dto.response.LevelThresholdVO;
import com.myblog.growth.interfaces.rest.dto.response.PointRuleVO;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 管理端：经验规则 & 积分规则配置接口.
 * <p>
 * 所有接口均需要 ADMIN 角色，否则返回 403。
 * </p>
 *
 * <pre>
 * 经验规则：
 *   GET  /api/admin/growth/rules          — 查询所有经验规则
 *   POST /api/admin/growth/rules          — 新增经验规则
 *   PUT  /api/admin/growth/rules          — 更新经验规则（乐观锁）
 *   GET  /api/admin/growth/thresholds     — 查询等级阈值列表
 *   PUT  /api/admin/growth/thresholds     — 批量保存等级阈值
 *
 * 积分规则：
 *   GET  /api/admin/growth/point-rules    — 查询所有积分规则（含已软删）
 *   POST /api/admin/growth/point-rules    — 新增积分规则
 *   PUT  /api/admin/growth/point-rules/{id} — 更新积分规则（乐观锁）
 *   DELETE /api/admin/growth/point-rules/{id} — 软删除积分规则
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/growth")
public class AdminGrowthRuleController {

    private final GrowthRuleAppService growthRuleAppService;
    private final PointRuleAppService pointRuleAppService;
    private final GrowthAssembler growthAssembler;

    public AdminGrowthRuleController(GrowthRuleAppService growthRuleAppService,
                                     PointRuleAppService pointRuleAppService,
                                     GrowthAssembler growthAssembler) {
        this.growthRuleAppService = growthRuleAppService;
        this.pointRuleAppService = pointRuleAppService;
        this.growthAssembler = growthAssembler;
    }

    // ─────────────────────── 经验规则管理 ───────────────────────

    /**
     * 查询所有已启用的经验规则.
     */
    @GetMapping("/rules")
    public Result<List<GrowthRuleVO>> listRules() {
        requireAdmin();
        List<GrowthRule> rules = growthRuleAppService.listAllRules();
        return Result.success(growthAssembler.toRuleVOList(rules));
    }

    /**
     * 新增经验规则.
     */
    @PostMapping("/rules")
    public Result<Long> createRule(@Valid @RequestBody SaveRuleRequest req) {
        requireAdmin();
        req.setOperator(AuthContext.getUsername());
        GrowthRule rule = growthAssembler.toDomain(req);
        Long id = growthRuleAppService.createRule(rule);
        return Result.success(id);
    }

    /**
     * 更新经验规则（乐观锁 CAS）.
     * <p>请求体中必须包含 id 和 version。</p>
     */
    @PutMapping("/rules")
    public Result<Void> updateRule(@Valid @RequestBody SaveRuleRequest req) {
        requireAdmin();
        if (req.getId() == null || req.getVersion() == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "更新规则时 id 和 version 不能为空");
        }
        req.setOperator(AuthContext.getUsername());
        GrowthRule rule = growthAssembler.toDomain(req);
        growthRuleAppService.updateRule(rule);
        return Result.success();
    }

    // ─────────────────────── 等级阈值管理 ───────────────────────

    /**
     * 查询所有已启用的等级阈值列表.
     */
    @GetMapping("/thresholds")
    public Result<List<LevelThresholdVO>> listThresholds() {
        requireAdmin();
        List<LevelThreshold> thresholds = growthRuleAppService.listThresholds();
        return Result.success(growthAssembler.toThresholdVOList(thresholds));
    }

    /**
     * 批量保存等级阈值（已存在则更新，不存在则插入）.
     */
    @PutMapping("/thresholds")
    public Result<Void> saveThresholds(@Valid @RequestBody BatchSaveThresholdRequest req) {
        requireAdmin();
        req.setOperator(AuthContext.getUsername());
        List<LevelThreshold> thresholds = growthAssembler.toDomainList(req);
        growthRuleAppService.saveThresholds(thresholds);
        return Result.success();
    }

    // ─────────────────────── 积分规则管理 ───────────────────────

    /**
     * 查询所有积分规则（含已软删）.
     */
    @GetMapping("/point-rules")
    public Result<List<PointRuleVO>> listPointRules() {
        requireAdmin();
        List<PointRule> rules = pointRuleAppService.listAllRules();
        return Result.success(growthAssembler.toPointRuleVOList(rules));
    }

    /**
     * 新增积分规则.
     */
    @PostMapping("/point-rules")
    public Result<Long> createPointRule(@Valid @RequestBody SavePointRuleRequest req) {
        requireAdmin();
        validatePointRule(req);
        req.setOperator(AuthContext.getUsername());
        PointRule rule = growthAssembler.toPointRuleDomain(req);
        Long id = pointRuleAppService.createRule(rule);
        return Result.success(id);
    }

    /**
     * 更新积分规则（乐观锁 CAS）.
     * <p>请求体中必须包含 id 和 version。</p>
     */
    @PutMapping("/point-rules/{id}")
    public Result<Void> updatePointRule(@PathVariable Long id,
                                        @Valid @RequestBody SavePointRuleRequest req) {
        requireAdmin();
        if (req.getVersion() == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "更新积分规则时 version 不能为空");
        }
        req.setId(id);
        req.setOperator(AuthContext.getUsername());
        validatePointRule(req);
        PointRule rule = growthAssembler.toPointRuleDomain(req);
        pointRuleAppService.updateRule(rule);
        return Result.success();
    }

    /**
     * 软删除积分规则（乐观锁 CAS）.
     *
     * @param id      规则 ID
     * @param version 当前版本号（必传）
     */
    @DeleteMapping("/point-rules/{id}")
    public Result<Void> deletePointRule(@PathVariable Long id,
                                        @RequestParam("version") int version) {
        requireAdmin();
        String operator = AuthContext.getUsername();
        pointRuleAppService.deleteRule(id, version, operator, "管理员删除");
        return Result.success();
    }

    /**
     * 校验积分规则字段合法性.
     */
    private void validatePointRule(SavePointRuleRequest req) {
        String sourceType = req.getSourceType();
        Integer pointAmount = req.getPointAmount();

        if ("UNLOCK_SHARE_RATIO".equals(sourceType)) {
            if (pointAmount == null || pointAmount < 0 || pointAmount > 100) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID,
                        "UNLOCK_SHARE_RATIO 的 pointAmount 必须在 0~100 之间");
            }
        } else if ("SIGN_IN".equals(sourceType) || "INVITE".equals(sourceType)
                || "PUBLISH".equals(sourceType) || "RECHARGE".equals(sourceType)) {
            if (pointAmount == null || pointAmount <= 0) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID,
                        sourceType + " 的 pointAmount 必须大于 0");
            }
        }

        if (req.getDailyLimit() < 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID,
                    "dailyLimit 不能小于 0");
        }
    }

    private void requireAdmin() {
        String role = AuthContext.getRole();
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }
}
