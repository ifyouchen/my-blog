package com.myblog.growth.application.listener;

import com.myblog.growth.application.event.LevelUpCheckEvent;
import com.myblog.growth.application.service.PointAppService;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.model.valueobject.RewardGrantLog;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.LevelRewardRepository;
import com.myblog.growth.domain.repository.RewardGrantLogRepository;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.domain.service.LevelPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 等级升级检查事件监听器.
 * <p>
 * 订阅 {@link LevelUpCheckEvent}（由 {@code ExpGrantAppService} 经验入账后发布），
 * 检查用户是否达到新的等级，若升级则更新 {@link GrowthAccount} 并发放等级奖励。
 * </p>
 *
 * <p>
 * <b>注意</b>：此监听器在 {@code ExpGrantAppService} 的事务上下文内同步执行
 * （不加 {@code @Async}），共享同一事务，确保经验入账与等级升级原子提交。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
@Component
public class LevelUpCheckEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LevelUpCheckEventHandler.class);

    private static final String REWARD_TYPE_LEVEL_UP = "LEVEL_UP";
    private static final String SOURCE_LEVEL_UP = "LEVEL_UP";

    private final GrowthAccountRepository growthAccountRepository;
    private final LevelThresholdRepository levelThresholdRepository;
    private final LevelPolicyService levelPolicyService;
    private final LevelRewardRepository levelRewardRepository;
    private final RewardGrantLogRepository rewardGrantLogRepository;
    private final PointAppService pointAppService;

    public LevelUpCheckEventHandler(GrowthAccountRepository growthAccountRepository,
                                     LevelThresholdRepository levelThresholdRepository,
                                     LevelPolicyService levelPolicyService,
                                     LevelRewardRepository levelRewardRepository,
                                     RewardGrantLogRepository rewardGrantLogRepository,
                                     PointAppService pointAppService) {
        this.growthAccountRepository = growthAccountRepository;
        this.levelThresholdRepository = levelThresholdRepository;
        this.levelPolicyService = levelPolicyService;
        this.levelRewardRepository = levelRewardRepository;
        this.rewardGrantLogRepository = rewardGrantLogRepository;
        this.pointAppService = pointAppService;
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onLevelUpCheck(LevelUpCheckEvent event) {
        Long userId = event.getUserId();
        int newExp = event.getNewExp();

        List<LevelThreshold> thresholds = levelThresholdRepository.findAllEnabled();
        if (thresholds.isEmpty()) {
            log.warn("[等级检查] 等级阈值配置为空，跳过检查。userId={}", userId);
            return;
        }

        int targetLevel = levelPolicyService.computeLevel(newExp, thresholds);

        GrowthAccount account = growthAccountRepository.findByUserId(userId)
                .orElse(null);
        if (account == null) {
            log.warn("[等级检查] 成长账户不存在，跳过。userId={}", userId);
            return;
        }

        boolean levelChanged = account.applyLevelUp(targetLevel);
        if (levelChanged) {
            int updated = growthAccountRepository.updateExpAndLevel(account);
            if (updated > 0) {
                log.info("[等级升级] 用户升级成功。userId={}, newLevel={}, exp={}", userId, targetLevel, newExp);
                grantLevelUpReward(userId, targetLevel);
            } else {
                log.warn("[等级检查] 乐观锁冲突，等级更新延迟。userId={}", userId);
            }
        }
    }

    /**
     * 发放等级升级奖励.
     *
     * @param userId    用户ID
     * @param newLevel  新等级
     */
    private void grantLevelUpReward(Long userId, int newLevel) {
        Optional<LevelRewardConfig> rewardOpt = levelRewardRepository.findByLevel(newLevel);
        if (!rewardOpt.isPresent()) {
            log.info("[等级奖励] 等级 {} 无奖励配置，跳过。userId={}", newLevel, userId);
            return;
        }

        LevelRewardConfig config = rewardOpt.get();
        if (config.getRewardPoints() <= 0) {
            log.info("[等级奖励] 等级 {} 奖励积分为 0，跳过。userId={}", newLevel, userId);
            return;
        }

        if (rewardGrantLogRepository.existsByUserAndReward(userId, REWARD_TYPE_LEVEL_UP, config.getId())) {
            log.info("[等级奖励] 等级 {} 奖励已发放，跳过。userId={}", newLevel, userId);
            return;
        }

        RewardGrantLog grantLog = RewardGrantLog.create(
                userId, REWARD_TYPE_LEVEL_UP, config.getId(),
                config.getRewardPoints(), "升级到 " + newLevel + " 级奖励");
        rewardGrantLogRepository.save(grantLog);

        String bizNo = "levelup-" + userId + "-" + newLevel;
        pointAppService.addPoints(userId, config.getRewardPoints(), SOURCE_LEVEL_UP, bizNo,
                "升级到 " + newLevel + " 级（" + config.getRewardTitle() + "）奖励", null);

        log.info("[等级奖励] 发放成功。userId={}, level={}, points={}, title={}",
                userId, newLevel, config.getRewardPoints(), config.getRewardTitle());
    }
}
