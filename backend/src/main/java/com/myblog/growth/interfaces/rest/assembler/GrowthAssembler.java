package com.myblog.growth.interfaces.rest.assembler;

import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import com.myblog.growth.domain.service.LevelPolicyService;
import com.myblog.growth.interfaces.rest.dto.request.BatchSaveThresholdRequest;
import com.myblog.growth.interfaces.rest.dto.request.SavePointRuleRequest;
import com.myblog.growth.interfaces.rest.dto.request.SaveRuleRequest;
import com.myblog.growth.interfaces.rest.dto.response.ExpJournalVO;
import com.myblog.growth.interfaces.rest.dto.response.GrowthAccountVO;
import com.myblog.growth.interfaces.rest.dto.response.GrowthRuleVO;
import com.myblog.growth.interfaces.rest.dto.response.LevelThresholdVO;
import com.myblog.growth.interfaces.rest.dto.response.PointRuleVO;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 成长模块 Assembler.
 * <p>
 * 负责 Domain 对象 ↔ VO，以及 Request ↔ Domain 对象之间的转换。
 * 不含任何业务逻辑。
 * </p>
 */
@Component
public class GrowthAssembler {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LevelPolicyService levelPolicyService;

    /**
     * 构造注入.
     *
     * @param levelPolicyService 等级策略领域服务（用于组装进度信息）
     */
    public GrowthAssembler(LevelPolicyService levelPolicyService) {
        this.levelPolicyService = levelPolicyService;
    }

    // ─────────────────── GrowthAccount → VO ───────────────────

    /**
     * 将成长账户聚合根和等级阈值列表组装为 {@link GrowthAccountVO}.
     *
     * @param account    成长账户聚合根
     * @param thresholds 等级阈值列表（按 level ASC）
     * @return 成长账户 VO
     */
    public GrowthAccountVO toVO(GrowthAccount account, List<LevelThreshold> thresholds) {
        GrowthAccountVO vo = new GrowthAccountVO();
        vo.setUserId(account.getUserId());
        vo.setCurrentExp(account.getCurrentExp());
        vo.setCurrentLevel(account.getCurrentLevel());
        vo.setLevelName(levelPolicyService.getLevelName(account.getCurrentLevel(), thresholds));
        vo.setProgressPercent(levelPolicyService.progressPercent(
                account.getCurrentExp(), account.getCurrentLevel(), thresholds));
        vo.setExpToNextLevel(levelPolicyService.expToNextLevel(
                account.getCurrentExp(), account.getCurrentLevel(), thresholds));
        return vo;
    }

    /**
     * 将成长账户、等级阈值和等级奖励列表组装为 {@link GrowthAccountVO}.
     *
     * @param account      成长账户聚合根
     * @param thresholds   等级阈值列表（按 level ASC）
     * @param levelRewards 等级奖励列表（按 level ASC）
     * @return 成长账户 VO
     */
    public GrowthAccountVO toVO(GrowthAccount account,
                                List<LevelThreshold> thresholds,
                                List<LevelRewardConfig> levelRewards) {
        GrowthAccountVO vo = toVO(account, thresholds);
        vo.setLevelRewards(toLevelRewardVOList(levelRewards, account.getCurrentLevel()));
        return vo;
    }

    /**
     * 将成长账户、等级阈值、等级奖励与权益状态组装为成长账户 VO.
     *
     * @param account           成长账户
     * @param thresholds        等级阈值
     * @param levelRewards      等级奖励列表
     * @param privilegeConfigs  等级权益配置
     * @param grantLogs         奖励发放记录
     * @param entitlements      已生效权益记录
     * @param registerGranted   注册奖励是否已发放
     * @return 成长账户 VO
     */
    public GrowthAccountVO toVO(GrowthAccount account,
                                List<LevelThreshold> thresholds,
                                List<LevelRewardConfig> levelRewards,
                                List<LevelPrivilegeConfig> privilegeConfigs,
                                List<RewardGrantLog> grantLogs,
                                List<UserPrivilegeEntitlement> entitlements,
                                boolean registerGranted) {
        GrowthAccountVO vo = toVO(account, thresholds);
        vo.setOwnedPrivilegeCodes(extractPrivilegeCodes(entitlements));
        vo.setLevelRewards(toLevelRewardVOList(
                levelRewards,
                privilegeConfigs,
                grantLogs,
                entitlements,
                account.getCurrentLevel(),
                registerGranted
        ));
        return vo;
    }

    private List<GrowthAccountVO.LevelRewardVO> toLevelRewardVOList(List<LevelRewardConfig> rewards,
                                                                     int currentLevel) {
        List<GrowthAccountVO.LevelRewardVO> voList = new ArrayList<>();
        if (rewards == null) {
            return voList;
        }
        for (LevelRewardConfig reward : rewards) {
            GrowthAccountVO.LevelRewardVO vo = new GrowthAccountVO.LevelRewardVO();
            vo.setLevel(reward.getLevel());
            vo.setRewardPoints(reward.getRewardPoints());
            vo.setRewardTitle(reward.getRewardTitle());
            vo.setDescription(reward.getDescription());
            vo.setAchieved(reward.getLevel() <= currentLevel);
            voList.add(vo);
        }
        return voList;
    }

    private List<GrowthAccountVO.LevelRewardVO> toLevelRewardVOList(List<LevelRewardConfig> rewards,
                                                                     List<LevelPrivilegeConfig> privilegeConfigs,
                                                                     List<RewardGrantLog> grantLogs,
                                                                     List<UserPrivilegeEntitlement> entitlements,
                                                                     int currentLevel,
                                                                     boolean registerGranted) {
        List<GrowthAccountVO.LevelRewardVO> voList = new ArrayList<GrowthAccountVO.LevelRewardVO>();
        if (rewards == null) {
            return voList;
        }
        Map<Integer, List<String>> privilegeCodesByLevel = buildPrivilegeCodesByLevel(privilegeConfigs);
        Set<Long> grantedRewardIds = new LinkedHashSet<Long>();
        Map<Long, String> grantTimeByRewardId = new HashMap<Long, String>();
        if (grantLogs != null) {
            for (RewardGrantLog grantLog : grantLogs) {
                grantedRewardIds.add(grantLog.getRewardId());
                if (grantLog.getGrantedAt() != null) {
                    grantTimeByRewardId.put(grantLog.getRewardId(), DATETIME_FORMATTER.format(grantLog.getGrantedAt()));
                }
            }
        }
        Set<String> activePrivilegeCodes = new LinkedHashSet<String>(extractPrivilegeCodes(entitlements));
        for (LevelRewardConfig reward : rewards) {
            GrowthAccountVO.LevelRewardVO vo = new GrowthAccountVO.LevelRewardVO();
            vo.setLevel(reward.getLevel());
            vo.setRewardPoints(reward.getRewardPoints());
            vo.setRewardTitle(reward.getRewardTitle());
            vo.setDescription(reward.getDescription());
            boolean achieved = reward.getLevel() <= currentLevel;
            vo.setAchieved(achieved);
            List<String> privilegeCodes = privilegeCodesByLevel.get(reward.getLevel());
            vo.setPrivilegeCodes(privilegeCodes == null ? Collections.<String>emptyList() : privilegeCodes);
            boolean pointsGranted = reward.getLevel() == 1
                    ? registerGranted
                    : reward.getRewardPoints() <= 0 || grantedRewardIds.contains(reward.getId());
            boolean privilegeGranted = privilegeCodes == null || privilegeCodes.isEmpty()
                    || activePrivilegeCodes.containsAll(privilegeCodes);
            vo.setRewardKind(resolveRewardKind(reward.getRewardPoints(), privilegeCodes));
            boolean granted = achieved && pointsGranted && privilegeGranted;
            vo.setStatus(granted ? "GRANTED" : "LOCKED");
            if (reward.getLevel() == 1 && registerGranted) {
                vo.setGrantedAt("已发放");
            } else {
                vo.setGrantedAt(grantTimeByRewardId.get(reward.getId()));
            }
            voList.add(vo);
        }
        return voList;
    }

    private String resolveRewardKind(int rewardPoints, List<String> privilegeCodes) {
        boolean hasPoints = rewardPoints > 0;
        boolean hasPrivileges = privilegeCodes != null && !privilegeCodes.isEmpty();
        if (hasPoints && hasPrivileges) {
            return "MIXED";
        }
        if (hasPrivileges) {
            return "PRIVILEGE";
        }
        return "POINTS";
    }

    private Map<Integer, List<String>> buildPrivilegeCodesByLevel(List<LevelPrivilegeConfig> privilegeConfigs) {
        Map<Integer, List<String>> result = new HashMap<Integer, List<String>>();
        if (privilegeConfigs == null) {
            return result;
        }
        for (LevelPrivilegeConfig privilegeConfig : privilegeConfigs) {
            List<String> codes = result.get(privilegeConfig.getLevel());
            if (codes == null) {
                codes = new ArrayList<String>();
                result.put(privilegeConfig.getLevel(), codes);
            }
            codes.add(privilegeConfig.getPrivilegeCode());
        }
        return result;
    }

    private List<String> extractPrivilegeCodes(List<UserPrivilegeEntitlement> entitlements) {
        List<String> codes = new ArrayList<String>();
        if (entitlements == null) {
            return codes;
        }
        for (UserPrivilegeEntitlement entitlement : entitlements) {
            if (entitlement.getPrivilegeCode() != null && !codes.contains(entitlement.getPrivilegeCode())) {
                codes.add(entitlement.getPrivilegeCode());
            }
        }
        return codes;
    }

    // ─────────────────── ExpJournal → VO ───────────────────

    /**
     * 将经验流水值对象转换为 {@link ExpJournalVO}.
     *
     * @param journal 经验流水值对象
     * @return 经验流水 VO
     */
    public ExpJournalVO toVO(ExpJournal journal) {
        ExpJournalVO vo = new ExpJournalVO();
        vo.setId(journal.getId());
        vo.setDelta(journal.getDelta());
        vo.setBalanceAfter(journal.getBalanceAfter());
        vo.setEventType(journal.getEventType());
        vo.setSourceId(journal.getSourceId());
        String grantRole = resolveGrantRole(journal);
        vo.setGrantRole(grantRole);
        vo.setGrantRoleLabel(grantRoleLabel(grantRole));
        vo.setRemark(journal.getRemark());
        vo.setCreatedAt(journal.getCreatedAt());
        return vo;
    }

    private String resolveGrantRole(ExpJournal journal) {
        String idempotentKey = journal.getIdempotentKey();
        if (idempotentKey != null) {
            String[] parts = idempotentKey.split(":");
            if (parts.length > 1 && isKnownGrantRole(parts[1])) {
                return parts[1];
            }
        }
        String remark = journal.getRemark();
        if (remark != null) {
            if (remark.startsWith("ACTOR")) {
                return "ACTOR";
            }
            if (remark.startsWith("AUTHOR")) {
                return "AUTHOR";
            }
        }
        return null;
    }

    private boolean isKnownGrantRole(String grantRole) {
        return "ACTOR".equals(grantRole) || "AUTHOR".equals(grantRole);
    }

    private String grantRoleLabel(String grantRole) {
        if ("ACTOR".equals(grantRole)) {
            return "你操作获得";
        }
        if ("AUTHOR".equals(grantRole)) {
            return "别人互动后你获得";
        }
        return null;
    }

    /**
     * 批量转换经验流水列表.
     *
     * @param journals 经验流水列表
     * @return VO 列表
     */
    public List<ExpJournalVO> toJournalVOList(List<ExpJournal> journals) {
        List<ExpJournalVO> voList = new ArrayList<>(journals.size());
        for (ExpJournal j : journals) {
            voList.add(toVO(j));
        }
        return voList;
    }

    // ─────────────────── GrowthRule ↔ VO ───────────────────

    /**
     * 将经验规则值对象转换为 {@link GrowthRuleVO}.
     *
     * @param rule 经验规则值对象
     * @return 规则 VO
     */
    public GrowthRuleVO toVO(GrowthRule rule) {
        GrowthRuleVO vo = new GrowthRuleVO();
        vo.setId(rule.getId());
        vo.setEventType(rule.getEventType());
        vo.setRole(rule.getRole());
        vo.setExpAmount(rule.getExpAmount());
        vo.setDailyLimit(rule.getDailyLimit());
        vo.setDailyLimitStrategy(rule.getDailyLimitStrategy());
        vo.setEnabled(rule.isEnabled());
        vo.setEffectiveAt(rule.getEffectiveAt());
        vo.setOperator(rule.getOperator());
        vo.setReason(rule.getReason());
        vo.setVersion(rule.getVersion());
        return vo;
    }

    /**
     * 批量转换规则列表.
     *
     * @param rules 规则列表
     * @return VO 列表
     */
    public List<GrowthRuleVO> toRuleVOList(List<GrowthRule> rules) {
        List<GrowthRuleVO> voList = new ArrayList<>(rules.size());
        for (GrowthRule r : rules) {
            voList.add(toVO(r));
        }
        return voList;
    }

    /**
     * 将 {@link SaveRuleRequest} 转换为领域值对象 {@link GrowthRule}.
     *
     * @param req 请求体
     * @return 规则值对象（id / version 取自请求体，可为 null）
     */
    public GrowthRule toDomain(SaveRuleRequest req) {
        return GrowthRule.of(
                req.getId(),
                req.getEventType(),
                req.getRole(),
                req.getExpAmount() == null ? 0 : req.getExpAmount(),
                req.getDailyLimit(),
                req.getDailyLimitStrategy(),
                req.isEnabled(),
                req.getEffectiveAt(),
                req.getOperator(),
                req.getReason(),
                req.getVersion() == null ? 0 : req.getVersion()
        );
    }

    // ─────────────────── PointRule ↔ VO ───────────────────

    /**
     * 将积分规则值对象转换为 {@link PointRuleVO}.
     *
     * @param rule 积分规则值对象
     * @return 积分规则 VO
     */
    public PointRuleVO toPointRuleVO(PointRule rule) {
        PointRuleVO vo = new PointRuleVO();
        vo.setId(rule.getId());
        vo.setSourceType(rule.getSourceType());
        vo.setPointAmount(rule.getPointAmount());
        vo.setDailyLimit(rule.getDailyLimit());
        vo.setEnabled(rule.isEnabled());
        vo.setEnabledLabel(rule.isEnabled() ? "启用" : "停用");
        vo.setDeleted(rule.isDeleted());
        vo.setDeletedAt(rule.getDeletedAt());
        if (rule.isDeleted()) {
            vo.setStatusLabel("已删除");
        } else {
            vo.setStatusLabel(rule.isEnabled() ? "正常" : "停用");
        }
        vo.setOperator(rule.getOperator());
        vo.setReason(rule.getReason());
        vo.setVersion(rule.getVersion());
        return vo;
    }

    /**
     * 批量转换积分规则列表.
     *
     * @param rules 积分规则列表
     * @return VO 列表
     */
    public List<PointRuleVO> toPointRuleVOList(List<PointRule> rules) {
        List<PointRuleVO> voList = new ArrayList<>(rules.size());
        for (PointRule r : rules) {
            voList.add(toPointRuleVO(r));
        }
        return voList;
    }

    /**
     * 将 {@link SavePointRuleRequest} 转换为领域值对象 {@link PointRule}.
     *
     * @param req 请求体
     * @return 积分规则值对象
     */
    public PointRule toPointRuleDomain(SavePointRuleRequest req) {
        return PointRule.of(
                req.getId(),
                req.getSourceType(),
                req.getPointAmount() == null ? 0 : req.getPointAmount(),
                req.getDailyLimit(),
                null,
                req.isEnabled(),
                null,
                req.getOperator(),
                req.getReason(),
                req.getVersion() == null ? 0 : req.getVersion(),
                null
        );
    }

    // ─────────────────── LevelThreshold ↔ VO ───────────────────

    /**
     * 将等级阈值值对象转换为 {@link LevelThresholdVO}.
     *
     * @param threshold 等级阈值值对象
     * @return 等级阈值 VO
     */
    public LevelThresholdVO toVO(LevelThreshold threshold) {
        LevelThresholdVO vo = new LevelThresholdVO();
        vo.setLevel(threshold.getLevel());
        vo.setMinExp(threshold.getMinExp());
        vo.setLevelName(threshold.getLevelName());
        vo.setDescription(threshold.getDescription());
        vo.setVersion(threshold.getVersion());
        return vo;
    }

    /**
     * 批量转换等级阈值列表.
     *
     * @param thresholds 等级阈值列表
     * @return VO 列表
     */
    public List<LevelThresholdVO> toThresholdVOList(List<LevelThreshold> thresholds) {
        List<LevelThresholdVO> voList = new ArrayList<>(thresholds.size());
        for (LevelThreshold t : thresholds) {
            voList.add(toVO(t));
        }
        return voList;
    }

    /**
     * 将 {@link BatchSaveThresholdRequest} 的条目转换为领域值对象列表.
     *
     * @param req 批量保存请求体
     * @return 等级阈值值对象列表
     */
    public List<LevelThreshold> toDomainList(BatchSaveThresholdRequest req) {
        List<LevelThreshold> list = new ArrayList<>(req.getThresholds().size());
        for (BatchSaveThresholdRequest.ThresholdItem item : req.getThresholds()) {
            list.add(LevelThreshold.of(
                    null,
                    item.getLevel(),
                    item.getMinExp(),
                    item.getLevelName(),
                    item.getDescription(),
                    true,
                    req.getOperator(),
                    item.getVersion()
            ));
        }
        return list;
    }
}

