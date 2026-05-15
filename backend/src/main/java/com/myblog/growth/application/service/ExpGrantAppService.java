package com.myblog.growth.application.service;

import com.myblog.growth.application.event.BehaviorExpEvent;
import com.myblog.growth.application.event.LevelUpCheckEvent;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.model.valueobject.GrowthRule;
import com.myblog.growth.domain.repository.DailyRewardCounterRepository;
import com.myblog.growth.domain.repository.EventConsumeRepository;
import com.myblog.growth.domain.repository.ExpJournalRepository;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.GrowthRuleRepository;
import com.myblog.growth.domain.service.ExpGrantDomainService;
import com.myblog.growth.shared.enums.ExpGrantRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 经验发放应用服务.
 * <p>
 * 负责将行为事件转化为经验入账，处理幂等、每日限额、乐观锁重试等横切关注点。
 * 所有写操作在单一事务内完成，任意步骤失败则全部回滚。
 * </p>
 *
 * <pre>
 * 核心流程：
 *   ① 幂等检查（event_consume_record）
 *   ② for ACTOR / AUTHOR：
 *      a. 查规则 → 不存在/未生效则跳过
 *      b. 查每日计数器 → 计算可发量
 *      c. 写经验流水（INSERT IGNORE）
 *      d. 更新成长账户（CAS 乐观锁，最多重试 3 次）
 *      e. 更新每日计数器
 *   ③ 发布 LevelUpCheckEvent
 *   ④ 标记幂等记录为 SUCCESS
 * </pre>
 */
@Service
public class ExpGrantAppService {

    private static final Logger log = LoggerFactory.getLogger(ExpGrantAppService.class);

    /** 乐观锁冲突时的最大重试次数. */
    private static final int MAX_RETRY = 3;

    /** 幂等消费者名称，对应 event_consume_record.consumer_name. */
    private static final String CONSUMER_NAME = "ExpGrantAppService";

    private final GrowthAccountRepository growthAccountRepository;
    private final ExpJournalRepository expJournalRepository;
    private final GrowthRuleRepository growthRuleRepository;
    private final DailyRewardCounterRepository dailyRewardCounterRepository;
    private final EventConsumeRepository eventConsumeRepository;
    private final ExpGrantDomainService expGrantDomainService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 构造注入所有依赖.
     */
    public ExpGrantAppService(GrowthAccountRepository growthAccountRepository,
                               ExpJournalRepository expJournalRepository,
                               GrowthRuleRepository growthRuleRepository,
                               DailyRewardCounterRepository dailyRewardCounterRepository,
                               EventConsumeRepository eventConsumeRepository,
                               ExpGrantDomainService expGrantDomainService,
                               ApplicationEventPublisher eventPublisher) {
        this.growthAccountRepository = growthAccountRepository;
        this.expJournalRepository = expJournalRepository;
        this.growthRuleRepository = growthRuleRepository;
        this.dailyRewardCounterRepository = dailyRewardCounterRepository;
        this.eventConsumeRepository = eventConsumeRepository;
        this.expGrantDomainService = expGrantDomainService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 根据行为事件发放经验.
     * <p>
     * 方法是幂等的：相同 eventId 重复调用，经验只入账一次。
     * </p>
     *
     * @param event 行为经验事件，包含 eventId、eventType、actorUserId、authorUserId、sourceId
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantExpForBehavior(BehaviorExpEvent event) {
        // ① 幂等检查：尝试插入 PROCESSING 状态
        int inserted = eventConsumeRepository.tryInsertProcessing(event.getEventId(), CONSUMER_NAME);
        if (inserted == 0) {
            log.info("[经验发放] 幂等跳过，eventId={}", event.getEventId());
            return;
        }

        try {
            // ② ACTOR 发放
            if (event.getActorUserId() != null) {
                grantForRole(event, ExpGrantRole.ACTOR, event.getActorUserId());
            }

            // ③ AUTHOR 发放（PUBLISH 行为不存在独立 AUTHOR，authorUserId 为 null 时跳过）
            if (event.getAuthorUserId() != null
                    && !event.getAuthorUserId().equals(event.getActorUserId())) {
                grantForRole(event, ExpGrantRole.AUTHOR, event.getAuthorUserId());
            }

            // ④ 标记成功
            eventConsumeRepository.markSuccess(event.getEventId(), CONSUMER_NAME);

        } catch (Exception e) {
            eventConsumeRepository.markFailed(event.getEventId(), CONSUMER_NAME, e.getMessage());
            throw e;
        }
    }

    /**
     * 为特定角色发放经验.
     *
     * @param event  行为事件
     * @param role   角色（ACTOR / AUTHOR）
     * @param userId 目标用户 ID
     */
    private void grantForRole(BehaviorExpEvent event, ExpGrantRole role, Long userId) {
        // 查规则
        Optional<GrowthRule> ruleOpt = growthRuleRepository.findByEventTypeAndRole(
                event.getEventType(), role.name());
        if (!ruleOpt.isPresent() || !ruleOpt.get().isEffective()) {
            log.debug("[经验发放] 规则不存在或未生效，eventType={}, role={}", event.getEventType(), role);
            return;
        }
        GrowthRule rule = ruleOpt.get();

        // 查每日已发量
        int grantedToday = dailyRewardCounterRepository.getGrantedExp(
                userId, event.getEventType(), LocalDate.now());

        // 计算可发量
        int grantAmount = expGrantDomainService.calcGrantAmount(rule, grantedToday);
        if (grantAmount <= 0) {
            log.debug("[经验发放] 已达每日上限，跳过。userId={}, eventType={}, role={}", userId, event.getEventType(), role);
            return;
        }

        // 获取或初始化成长账户
        GrowthAccount account = growthAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    GrowthAccount newAccount = GrowthAccount.create(userId);
                    growthAccountRepository.save(newAccount);
                    return growthAccountRepository.findByUserId(userId).orElseThrow(
                            () -> new IllegalStateException("成长账户初始化失败，userId=" + userId));
                });

        // 更新账户经验（乐观锁重试）
        account.addExp(grantAmount);
        boolean updated = false;
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows = growthAccountRepository.updateExpAndLevel(account);
            if (rows > 0) {
                updated = true;
                break;
            }
            // 版本冲突：重新读取最新版本再次尝试
            account = growthAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("成长账户不存在，userId=" + userId));
            account.addExp(grantAmount);
        }
        if (!updated) {
            throw new RuntimeException("经验更新乐观锁重试耗尽，userId=" + userId);
        }

        // 写经验流水
        String idempotentKey = event.getEventType() + ":" + role.name() + ":" + userId
                + ":" + event.getSourceId() + ":" + event.getEventId();
        ExpJournal journal = ExpJournal.create(
                userId, grantAmount, account.getCurrentExp(),
                event.getEventType(), event.getSourceId(),
                role.name() + " 行为经验", idempotentKey);
        expJournalRepository.insertIgnore(journal);

        // 更新每日计数器
        dailyRewardCounterRepository.incrementCounter(
                userId, event.getEventType(), LocalDate.now(), 1, grantAmount);

        // 发布等级检查事件
        eventPublisher.publishEvent(new LevelUpCheckEvent(userId, account.getCurrentExp()));

        log.info("[经验发放] 成功。userId={}, eventType={}, role={}, delta={}",
                userId, event.getEventType(), role, grantAmount);
    }
}

