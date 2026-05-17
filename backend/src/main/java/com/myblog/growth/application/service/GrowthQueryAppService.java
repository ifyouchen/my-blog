package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import com.myblog.growth.domain.repository.ExpJournalRepository;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.LevelRewardRepository;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.domain.repository.PointJournalRepository;
import com.myblog.growth.domain.repository.RewardGrantLogRepository;
import com.myblog.growth.domain.repository.UserPrivilegeEntitlementRepository;
import com.myblog.growth.domain.service.LevelPolicyService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 成长信息查询应用服务.
 * <p>
 * 汇聚等级进度、经验流水等查询能力，供 GrowthController 调用。
 * 所有方法均为只读，不需要事务注解。
 * </p>
 */
@Service
public class GrowthQueryAppService {

    /** 查询近期经验流水时的默认条数. */
    private static final int DEFAULT_JOURNAL_LIMIT = 20;

    private final GrowthAccountRepository growthAccountRepository;
    private final ExpJournalRepository expJournalRepository;
    private final LevelThresholdRepository levelThresholdRepository;
    private final LevelRewardRepository levelRewardRepository;
    private final LevelPolicyService levelPolicyService;
    private final RewardGrantLogRepository rewardGrantLogRepository;
    private final UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository;
    private final PointJournalRepository pointJournalRepository;

    /**
     * 构造注入所有依赖.
     */
    public GrowthQueryAppService(GrowthAccountRepository growthAccountRepository,
                                  ExpJournalRepository expJournalRepository,
                                  LevelThresholdRepository levelThresholdRepository,
                                  LevelRewardRepository levelRewardRepository,
                                  LevelPolicyService levelPolicyService,
                                  RewardGrantLogRepository rewardGrantLogRepository,
                                  UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository,
                                  PointJournalRepository pointJournalRepository) {
        this.growthAccountRepository = growthAccountRepository;
        this.expJournalRepository = expJournalRepository;
        this.levelThresholdRepository = levelThresholdRepository;
        this.levelRewardRepository = levelRewardRepository;
        this.levelPolicyService = levelPolicyService;
        this.rewardGrantLogRepository = rewardGrantLogRepository;
        this.userPrivilegeEntitlementRepository = userPrivilegeEntitlementRepository;
        this.pointJournalRepository = pointJournalRepository;
    }

    /**
     * 查询用户成长账户信息（等级、经验、下一级所需经验等）.
     * <p>
     * 若成长账户不存在，返回一个初始化但未持久化的默认账户。
     * </p>
     *
     * @param userId 用户 ID
     * @return 成长账户聚合根（只读）
     */
    public GrowthAccount getGrowthAccount(Long userId) {
        return growthAccountRepository.findByUserId(userId)
                .orElseGet(() -> GrowthAccount.create(userId));
    }

    /**
     * 计算用户在当前等级的经验进度（百分比）.
     * <p>
     * 返回值为 0 ~ 100 的整数，最高等级时固定返回 100。
     * </p>
     *
     * @param userId 用户 ID
     * @return 等级进度（0 ~ 100）
     */
    public int getLevelProgress(Long userId) {
        GrowthAccount account = getGrowthAccount(userId);
        List<LevelThreshold> thresholds = levelThresholdRepository.findAllEnabled();
        return levelPolicyService.progressPercent(account.getCurrentExp(), account.getCurrentLevel(), thresholds);
    }

    /**
     * 查询用户近期经验流水（最多 {@value #DEFAULT_JOURNAL_LIMIT} 条，按时间倒序）.
     *
     * @param userId 用户 ID
     * @return 经验流水列表
     */
    public List<ExpJournal> getRecentJournals(Long userId) {
        return expJournalRepository.findRecentByUserId(userId, DEFAULT_JOURNAL_LIMIT);
    }

    /**
     * 查询用户近期经验流水，支持自定义返回条数.
     *
     * @param userId 用户 ID
     * @param limit  最多返回条数（最大值由调用方保证，建议 ≤ 50）
     * @return 经验流水列表
     */
    public List<ExpJournal> getRecentJournals(Long userId, int limit) {
        return expJournalRepository.findRecentByUserId(userId, limit);
    }

    /**
     * 查询普通用户可见的等级奖励配置.
     *
     * @return 已启用等级奖励列表
     */
    public List<LevelRewardConfig> getVisibleLevelRewards() {
        return levelRewardRepository.findAllEnabled();
    }

    /**
     * 查询用户当前已发放的等级奖励记录.
     *
     * @param userId 用户ID
     * @return 奖励记录列表
     */
    public List<RewardGrantLog> getRewardGrantLogs(Long userId) {
        return rewardGrantLogRepository.findByUserId(userId);
    }

    /**
     * 查询用户当前有效权益记录.
     *
     * @param userId 用户ID
     * @return 权益记录列表
     */
    public List<UserPrivilegeEntitlement> getActiveEntitlements(Long userId) {
        return userPrivilegeEntitlementRepository.findActiveByUserId(userId);
    }

    /**
     * 判断注册奖励是否已发放.
     *
     * @param userId 用户ID
     * @return true 表示已发放
     */
    public boolean hasRegisterBonus(Long userId) {
        return pointJournalRepository.findByBizNo("REGISTER:" + userId).isPresent();
    }

    /**
     * 查询用户当前拥有的权益编码列表.
     *
     * @param userId 用户ID
     * @return 权益编码列表
     */
    public List<String> getOwnedPrivilegeCodes(Long userId) {
        List<String> codes = new ArrayList<String>();
        for (UserPrivilegeEntitlement entitlement : userPrivilegeEntitlementRepository.findActiveByUserId(userId)) {
            codes.add(entitlement.getPrivilegeCode());
        }
        return codes;
    }
}

