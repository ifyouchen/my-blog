package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.repository.PointRuleRepository;
import com.myblog.growth.domain.service.InviteRiskControlService;
import com.myblog.growth.infrastructure.repository.persistence.repository.InviteRelationRepositoryImpl;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 拉新奖励应用服务.
 *
 * <p>
 * 负责在新用户注册完成后触发邀请奖励，处理幂等、风控、积分入账全流程。
 * </p>
 *
 * <pre>
 * 核心流程 triggerReward()：
 *   ① 校验邀请人/受邀人有效性（允许外部传入，实际用户存在校验由 Controller 或上层保证）
 *   ② InviteRiskControlService.check(inviterUserId) → 风控
 *   ③ 查 invite_relation WHERE invitee_user_id = ? → 已存在则返回幂等成功
 *   ④ INSERT IGNORE invite_relation(PENDING)
 *   ⑤ 查 point_rule_config WHERE source_type = INVITE → 未启用则跳过
 *   ⑥ PointAppService.addPoints(inviterUserId, pointAmount, "INVITE", bizNo)
 *   ⑦ UPDATE invite_relation SET reward_status = GRANTED
 * </pre>
 */
@Service
public class InviteRewardAppService {

    private static final Logger log = LoggerFactory.getLogger(InviteRewardAppService.class);

    /** 积分规则来源类型：邀请. */
    private static final String SOURCE_INVITE = "INVITE";

    private final InviteRelationRepositoryImpl inviteRelationRepository;
    private final PointRuleRepository pointRuleRepository;
    private final InviteRiskControlService inviteRiskControlService;
    private final PointAppService pointAppService;

    /**
     * 构造注入.
     *
     * @param inviteRelationRepository 邀请关系 Repository 实现
     * @param pointRuleRepository      积分规则 Repository
     * @param inviteRiskControlService 风控服务
     * @param pointAppService          积分应用服务
     */
    public InviteRewardAppService(InviteRelationRepositoryImpl inviteRelationRepository,
                                  PointRuleRepository pointRuleRepository,
                                  InviteRiskControlService inviteRiskControlService,
                                  PointAppService pointAppService) {
        this.inviteRelationRepository = inviteRelationRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.inviteRiskControlService = inviteRiskControlService;
        this.pointAppService = pointAppService;
    }

    /**
     * 奖励触发结果值对象.
     */
    public static class InviteRewardResult {
        private final Long inviterUserId;
        private final Long inviteeUserId;
        private final int pointsGranted;
        private final String rewardStatus;
        private final String message;

        public InviteRewardResult(Long inviterUserId, Long inviteeUserId,
                                  int pointsGranted, String rewardStatus, String message) {
            this.inviterUserId = inviterUserId;
            this.inviteeUserId = inviteeUserId;
            this.pointsGranted = pointsGranted;
            this.rewardStatus = rewardStatus;
            this.message = message;
        }

        public Long getInviterUserId() { return inviterUserId; }
        public Long getInviteeUserId() { return inviteeUserId; }
        public int getPointsGranted() { return pointsGranted; }
        public String getRewardStatus() { return rewardStatus; }
        public String getMessage() { return message; }
    }

    /**
     * 触发拉新奖励.
     *
     * @param inviterUserId 邀请人用户 ID
     * @param inviteeUserId 被邀请人用户 ID
     * @return 奖励触发结果
     */
    @Transactional(rollbackFor = Exception.class)
    public InviteRewardResult triggerReward(Long inviterUserId, Long inviteeUserId) {
        if (inviterUserId == null || inviteeUserId == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "邀请人和受邀人 ID 不能为空");
        }
        if (inviterUserId.equals(inviteeUserId)) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "邀请人和受邀人不能是同一用户");
        }

        // ③ 幂等检查：受邀人已存在邀请记录
        if (inviteRelationRepository.existsByInviteeId(inviteeUserId)) {
            log.info("[拉新奖励] 幂等跳过，inviteeUserId={}", inviteeUserId);
            return new InviteRewardResult(inviterUserId, inviteeUserId, 0,
                    "GRANTED", "已奖励，不重复发放");
        }

        // ② 风控校验
        inviteRiskControlService.check(inviterUserId);

        // ④ INSERT IGNORE invite_relation(PENDING)
        int inserted = inviteRelationRepository.insertIgnore(inviterUserId, inviteeUserId);
        if (inserted == 0) {
            // 并发场景：其他线程已插入
            log.info("[拉新奖励] 并发幂等跳过，inviteeUserId={}", inviteeUserId);
            return new InviteRewardResult(inviterUserId, inviteeUserId, 0,
                    "GRANTED", "已奖励，不重复发放");
        }

        // ⑤ 查积分规则
        Optional<PointRule> ruleOpt = pointRuleRepository.findBySourceType(SOURCE_INVITE);
        if (!ruleOpt.isPresent() || !ruleOpt.get().isEffective()) {
            log.info("[拉新奖励] INVITE 积分规则未启用，跳过发奖。inviterUserId={}", inviterUserId);
            return new InviteRewardResult(inviterUserId, inviteeUserId, 0,
                    "SKIPPED", "积分规则未启用");
        }
        int pointAmount = ruleOpt.get().getPointAmount();

        // ⑥ 积分入账
        String bizNo = "invite-" + inviterUserId + "-" + inviteeUserId;
        pointAppService.addPoints(inviterUserId, pointAmount, SOURCE_INVITE, bizNo,
                "邀请新用户注册奖励", null);

        // ⑦ 更新状态为 GRANTED
        inviteRelationRepository.markGranted(inviteeUserId, bizNo);

        log.info("[拉新奖励] 成功。inviterUserId={}, inviteeUserId={}, pointsGranted={}",
                inviterUserId, inviteeUserId, pointAmount);

        return new InviteRewardResult(inviterUserId, inviteeUserId, pointAmount, "GRANTED", null);
    }

    /**
     * 查询邀请汇总.
     *
     * @param inviterUserId 邀请人用户 ID
     * @return 邀请汇总信息
     */
    public InviteSummary getSummary(Long inviterUserId) {
        int totalInvited = inviteRelationRepository.countGrantedByInviterId(inviterUserId);
        return new InviteSummary(inviterUserId, totalInvited);
    }

    /**
     * 邀请汇总值对象.
     */
    public static class InviteSummary {
        private final Long inviterUserId;
        private final int totalGrantedCount;

        public InviteSummary(Long inviterUserId, int totalGrantedCount) {
            this.inviterUserId = inviterUserId;
            this.totalGrantedCount = totalGrantedCount;
        }

        public Long getInviterUserId() { return inviterUserId; }
        public int getTotalGrantedCount() { return totalGrantedCount; }
    }
}

