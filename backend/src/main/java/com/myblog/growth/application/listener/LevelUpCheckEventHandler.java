package com.myblog.growth.application.listener;

import com.myblog.growth.application.event.LevelUpCheckEvent;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.LevelThreshold;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.LevelThresholdRepository;
import com.myblog.growth.domain.service.LevelPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 等级升级检查事件监听器.
 * <p>
 * 订阅 {@link LevelUpCheckEvent}（由 {@code ExpGrantAppService} 经验入账后发布），
 * 检查用户是否达到新的等级，若升级则更新 {@link GrowthAccount}。
 * </p>
 *
 * <p>
 * <b>注意</b>：此监听器在 {@code ExpGrantAppService} 的事务上下文内同步执行
 * （不加 {@code @Async}），共享同一事务，确保经验入账与等级升级原子提交。
 * </p>
 */
@Component
public class LevelUpCheckEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LevelUpCheckEventHandler.class);

    private final GrowthAccountRepository growthAccountRepository;
    private final LevelThresholdRepository levelThresholdRepository;
    private final LevelPolicyService levelPolicyService;

    /**
     * 构造注入.
     *
     * @param growthAccountRepository  成长账户 Repository
     * @param levelThresholdRepository 等级阈值 Repository
     * @param levelPolicyService       等级策略领域服务
     */
    public LevelUpCheckEventHandler(GrowthAccountRepository growthAccountRepository,
                                     LevelThresholdRepository levelThresholdRepository,
                                     LevelPolicyService levelPolicyService) {
        this.growthAccountRepository = growthAccountRepository;
        this.levelThresholdRepository = levelThresholdRepository;
        this.levelPolicyService = levelPolicyService;
    }

    /**
     * 处理等级检查事件.
     * <p>
     * 流程：
     * <ol>
     *   <li>加载全量等级阈值（生产环境可加 Caffeine 缓存优化）</li>
     *   <li>根据当前经验值计算应达等级</li>
     *   <li>若新等级 &gt; 旧等级，调用聚合根 {@code applyLevelUp()} 并更新数据库</li>
     * </ol>
     * </p>
     *
     * @param event 等级检查事件，包含 userId 和最新经验值
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onLevelUpCheck(LevelUpCheckEvent event) {
        Long userId = event.getUserId();
        int newExp = event.getNewExp();

        // 加载等级阈值（已按 level ASC 排列）
        List<LevelThreshold> thresholds = levelThresholdRepository.findAllEnabled();
        if (thresholds.isEmpty()) {
            log.warn("[等级检查] 等级阈值配置为空，跳过检查。userId={}", userId);
            return;
        }

        // 计算目标等级
        int targetLevel = levelPolicyService.computeLevel(newExp, thresholds);

        // 查询当前账户
        GrowthAccount account = growthAccountRepository.findByUserId(userId)
                .orElse(null);
        if (account == null) {
            log.warn("[等级检查] 成长账户不存在，跳过。userId={}", userId);
            return;
        }

        // 是否需要升级
        boolean levelChanged = account.applyLevelUp(targetLevel);
        if (levelChanged) {
            int updated = growthAccountRepository.updateExpAndLevel(account);
            if (updated > 0) {
                log.info("[等级升级] 用户升级成功。userId={}, newLevel={}, exp={}", userId, targetLevel, newExp);
            } else {
                // 乐观锁冲突：下次经验入账时会再次触发检查，最终一致
                log.warn("[等级检查] 乐观锁冲突，等级更新延迟。userId={}", userId);
            }
        }
    }
}

