package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.GrowthRuleAppService;
import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.interfaces.rest.assembler.GrowthAssembler;
import com.myblog.growth.interfaces.rest.dto.request.BatchSaveThresholdRequest;
import com.myblog.growth.interfaces.rest.dto.request.SaveRuleRequest;
import com.myblog.growth.interfaces.rest.dto.response.GrowthRuleVO;
import com.myblog.growth.interfaces.rest.dto.response.LevelThresholdVO;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 管理端：经验规则配置接口.
 * <p>
 * 所有接口均需要 ADMIN 角色，否则返回 403。
 * </p>
 *
 * <pre>
 * GET  /api/admin/growth/rules      — 查询所有经验规则
 * POST /api/admin/growth/rules      — 新增经验规则
 * PUT  /api/admin/growth/rules      — 更新经验规则（乐观锁）
 * GET  /api/admin/growth/thresholds — 查询等级阈值列表
 * PUT  /api/admin/growth/thresholds — 批量保存等级阈值
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/growth")
public class AdminGrowthRuleController {

    private final GrowthRuleAppService growthRuleAppService;
    private final GrowthAssembler growthAssembler;

    /**
     * 构造注入.
     *
     * @param growthRuleAppService 经验规则管理应用服务
     * @param growthAssembler      成长模块 Assembler
     */
    public AdminGrowthRuleController(GrowthRuleAppService growthRuleAppService,
                                      GrowthAssembler growthAssembler) {
        this.growthRuleAppService = growthRuleAppService;
        this.growthAssembler = growthAssembler;
    }

    // ─────────────────────── 规则管理 ───────────────────────

    /**
     * 查询所有已启用的经验规则.
     *
     * @return 规则 VO 列表
     */
    @GetMapping("/rules")
    public Result<List<GrowthRuleVO>> listRules() {
        requireAdmin();
        List<GrowthRule> rules = growthRuleAppService.listAllRules();
        return Result.success(growthAssembler.toRuleVOList(rules));
    }

    /**
     * 新增经验规则.
     *
     * @param req 新增规则请求体
     * @return 新规则 ID
     */
    @PostMapping("/rules")
    public Result<Long> createRule(@Valid @RequestBody SaveRuleRequest req) {
        requireAdmin();
        // 操作人注入
        req.setOperator(AuthContext.getUsername());
        GrowthRule rule = growthAssembler.toDomain(req);
        Long id = growthRuleAppService.createRule(rule);
        return Result.success(id);
    }

    /**
     * 更新经验规则（乐观锁 CAS）.
     * <p>请求体中必须包含 id 和 version。</p>
     *
     * @param req 更新规则请求体
     * @return 无返回值
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
     *
     * @return 等级阈值 VO 列表（按 level ASC）
     */
    @GetMapping("/thresholds")
    public Result<List<LevelThresholdVO>> listThresholds() {
        requireAdmin();
        List<LevelThreshold> thresholds = growthRuleAppService.listThresholds();
        return Result.success(growthAssembler.toThresholdVOList(thresholds));
    }

    /**
     * 批量保存等级阈值（已存在则更新，不存在则插入）.
     *
     * @param req 批量保存请求体
     * @return 无返回值
     */
    @PutMapping("/thresholds")
    public Result<Void> saveThresholds(@Valid @RequestBody BatchSaveThresholdRequest req) {
        requireAdmin();
        req.setOperator(AuthContext.getUsername());
        List<LevelThreshold> thresholds = growthAssembler.toDomainList(req);
        growthRuleAppService.saveThresholds(thresholds);
        return Result.success();
    }

    /**
     * 校验当前请求用户是否为 ADMIN，否则抛出 403.
     * <p>使用 {@link UserRole#ADMIN} 枚举进行比较，避免魔法字符串。</p>
     */
    private void requireAdmin() {
        String role = AuthContext.getRole();
        if (!UserRole.ADMIN.name().equals(role)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }
}

