package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.repository.ExpJournalRepository;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.domain.service.LevelPolicyService;
import org.springframework.stereotype.Service;

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
    private final LevelPolicyService levelPolicyService;

    /**
     * 构造注入所有依赖.
     */
    public GrowthQueryAppService(GrowthAccountRepository growthAccountRepository,
                                  ExpJournalRepository expJournalRepository,
                                  LevelThresholdRepository levelThresholdRepository,
                                  LevelPolicyService levelPolicyService) {
        this.growthAccountRepository = growthAccountRepository;
        this.expJournalRepository = expJournalRepository;
        this.levelThresholdRepository = levelThresholdRepository;
        this.levelPolicyService = levelPolicyService;
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
}

