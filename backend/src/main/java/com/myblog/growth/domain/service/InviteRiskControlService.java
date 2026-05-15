package com.myblog.growth.domain.service;

import com.myblog.growth.infrastructure.repository.persistence.repository.InviteRelationRepositoryImpl;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.springframework.stereotype.Service;

/**
 * 邀请风控服务.
 *
 * <p>
 * 负责校验拉新奖励的风控规则，防止刷量和羊毛行为。
 * 当前版本仅实现每日频次上限（10次/天），IP/设备指纹校验需引入 Redis 后扩展。
 * </p>
 *
 * <p>风控规则（可配置化）：</p>
 * <ul>
 *   <li>邀请人每日最多获得奖励 10 次</li>
 *   <li>同一受邀人只能被邀请一次（由 uk_invitee 数据库唯一索引保证）</li>
 *   <li>风控总开关：point_rule_config.source_type=INVITE 的 enabled 字段</li>
 * </ul>
 */
@Service
public class InviteRiskControlService {

    /** 单人每日最多邀请奖励次数. */
    private static final int DAILY_INVITE_LIMIT = 10;

    private final InviteRelationRepositoryImpl inviteRelationRepository;

    /**
     * 构造注入.
     *
     * @param inviteRelationRepository 邀请关系 Repository 实现
     */
    public InviteRiskControlService(InviteRelationRepositoryImpl inviteRelationRepository) {
        this.inviteRelationRepository = inviteRelationRepository;
    }

    /**
     * 执行邀请风控校验.
     *
     * @param inviterUserId 邀请人用户 ID
     * @throws GrowthBusinessException 风控拦截时抛出
     */
    public void check(Long inviterUserId) {
        // 检查邀请人今日奖励次数
        int todayCount = inviteRelationRepository.countGrantedTodayByInviterId(inviterUserId);
        if (todayCount >= DAILY_INVITE_LIMIT) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID,
                    "风控拦截：今日邀请奖励次数已达上限（" + DAILY_INVITE_LIMIT + "次）");
        }
    }
}

