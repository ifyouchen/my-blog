package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.repository.GrowthRuleRepository;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 经验规则管理应用服务.
 * <p>
 * 供管理端接口调用，负责规则的增改查、等级阈值批量保存。
 * 所有写操作加事务。
 * </p>
 */
@Service
public class GrowthRuleAppService {

    private static final Logger log = LoggerFactory.getLogger(GrowthRuleAppService.class);

    private final GrowthRuleRepository growthRuleRepository;
    private final LevelThresholdRepository levelThresholdRepository;

    /**
     * 构造注入所有依赖.
     */
    public GrowthRuleAppService(GrowthRuleRepository growthRuleRepository,
                                  LevelThresholdRepository levelThresholdRepository) {
        this.growthRuleRepository = growthRuleRepository;
        this.levelThresholdRepository = levelThresholdRepository;
    }

    // ─────────────────────────── 规则查询 ───────────────────────────

    /**
     * 查询所有已启用且生效的经验规则.
     *
     * @return 经验规则列表
     */
    public List<GrowthRule> listAllRules() {
        return growthRuleRepository.findAllEnabled();
    }

    // ─────────────────────────── 规则新增 ───────────────────────────

    /**
     * 新增经验规则.
     * <p>
     * 同一 eventType + role 不允许重复配置。
     * </p>
     *
     * @param rule 新规则（id 应为 null）
     * @return 新规则的数据库 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(GrowthRule rule) {
        validateRule(rule);
        // 检查是否已存在相同 eventType + role 的规则
        growthRuleRepository.findByEventTypeAndRole(rule.getEventType(), rule.getRole())
                .ifPresent(existing -> {
                    throw new GrowthBusinessException(GrowthErrorCode.RULE_DUPLICATE,
                            "规则已存在：eventType=" + rule.getEventType() + ", role=" + rule.getRole());
                });
        Long id = growthRuleRepository.insert(rule);
        log.info("[规则管理] 新增规则成功。id={}, eventType={}, role={}", id, rule.getEventType(), rule.getRole());
        return id;
    }

    // ─────────────────────────── 规则更新 ───────────────────────────

    /**
     * 更新经验规则（乐观锁 CAS）.
     *
     * @param rule 包含最新字段和当前版本号的规则对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(GrowthRule rule) {
        validateRule(rule);
        growthRuleRepository.findByEventTypeAndRole(rule.getEventType(), rule.getRole())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(rule.getId())) {
                        throw new GrowthBusinessException(GrowthErrorCode.RULE_DUPLICATE,
                                "规则已存在：eventType=" + rule.getEventType() + ", role=" + rule.getRole());
                    }
                });
        int updated = growthRuleRepository.update(rule);
        if (updated == 0) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "规则更新失败，可能已被其他人修改，请刷新后重试。id=" + rule.getId());
        }
        log.info("[规则管理] 更新规则成功。id={}", rule.getId());
    }

    /**
     * 软删除经验规则.
     *
     * @param id       规则 ID
     * @param version  当前版本号
     * @param operator 操作人
     * @param reason   删除原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id, int version, String operator, String reason) {
        int deleted = growthRuleRepository.softDelete(id, version, operator, reason);
        if (deleted == 0) {
            throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                    "规则删除失败，可能已被其他人修改，请刷新后重试。id=" + id);
        }
        log.info("[规则管理] 软删除规则成功。id={}, operator={}", id, operator);
    }

    // ─────────────────────────── 等级阈值 ───────────────────────────

    /**
     * 查询所有已启用的等级阈值.
     *
     * @return 等级阈值列表（按 level ASC）
     */
    public List<LevelThreshold> listThresholds() {
        return levelThresholdRepository.findAllEnabled();
    }

    /**
     * 批量保存等级阈值（已存在则更新，不存在则插入）.
     * <p>
     * 传入列表不能为空，且每个阈值的 level 应 ≥ 1。
     * </p>
     *
     * @param thresholds 等级阈值列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveThresholds(List<LevelThreshold> thresholds) {
        validateThresholds(thresholds);
        int count = levelThresholdRepository.batchSave(thresholds);
        log.info("[规则管理] 批量保存等级阈值成功，条数={}", count);
    }

    private void validateRule(GrowthRule rule) {
        if (rule == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "经验规则不能为空");
        }
        if (rule.getExpAmount() <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "单次经验必须大于 0");
        }
        if (rule.getDailyLimit() < 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "每日上限次数不能小于 0");
        }
        String strategy = rule.getDailyLimitStrategy();
        if (!"SKIP".equals(strategy) && !"PARTIAL".equals(strategy)) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "不支持的超限策略：" + strategy);
        }
    }

    private void validateThresholds(List<LevelThreshold> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级阈值列表不能为空");
        }
        Set<Integer> levels = new HashSet<>();
        List<LevelThreshold> sorted = thresholds.stream()
                .sorted(Comparator.comparingInt(LevelThreshold::getLevel))
                .collect(Collectors.toList());
        int lastMinExp = -1;
        for (LevelThreshold threshold : sorted) {
            int level = threshold.getLevel();
            int minExp = threshold.getMinExp();
            if (level <= 0) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级必须大于 0");
            }
            if (!levels.add(level)) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级重复：Lv." + level);
            }
            if (minExp < 0) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "最低经验不能小于 0");
            }
            if (minExp < lastMinExp) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "最低经验需要随等级递增");
            }
            if (threshold.getLevelName() == null || threshold.getLevelName().trim().isEmpty()) {
                throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "等级名称不能为空");
            }
            lastMinExp = minExp;
        }
    }
}

