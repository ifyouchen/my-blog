package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.aggregate.PointAccount;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.domain.repository.PointAccountRepository;
import com.myblog.growth.domain.repository.PointJournalRepository;
import com.myblog.growth.infrastructure.repository.persistence.entity.AdminPointAdjustLogDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.AdminPointAdjustLogMapper;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 管理员积分调整应用服务.
 *
 * <p>
 * 支持正负双向调整，扣分前校验余额确保不出现负数。
 * 每次调整均写入 admin_point_adjust_log 审计日志与 user_point_journal 流水。
 * bizNo 唯一索引保证幂等性。
 * </p>
 *
 * <pre>
 * 核心流程：
 *   ① 参数校验（reason 必填、delta != 0）
 *   ② 查询或初始化积分账户
 *   ③ 扣分时校验余额是否充足（禁负策略）
 *   ④ 调用聚合根 adjust() 方法执行调整
 *   ⑤ CAS 乐观锁更新账户（最多重试 3 次）
 *   ⑥ INSERT IGNORE 写入积分流水（幂等）
 *   ⑦ 写入管理员调分审计日志
 * </pre>
 */
@Service
public class AdminPointAppService {

    private static final Logger log = LoggerFactory.getLogger(AdminPointAppService.class);

    /** 乐观锁冲突时的最大重试次数. */
    private static final int MAX_RETRY = 3;

    /** 积分流水来源类型：管理员调整. */
    private static final String SOURCE_ADMIN_ADJUST = "ADMIN_ADJUST";

    private final PointAccountRepository pointAccountRepository;
    private final PointJournalRepository pointJournalRepository;
    private final AdminPointAdjustLogMapper adminPointAdjustLogMapper;

    /**
     * 构造注入依赖.
     *
     * @param pointAccountRepository    积分账户 Repository
     * @param pointJournalRepository    积分流水 Repository
     * @param adminPointAdjustLogMapper 管理员调分日志 Mapper
     */
    public AdminPointAppService(PointAccountRepository pointAccountRepository,
                                PointJournalRepository pointJournalRepository,
                                AdminPointAdjustLogMapper adminPointAdjustLogMapper) {
        this.pointAccountRepository = pointAccountRepository;
        this.pointJournalRepository = pointJournalRepository;
        this.adminPointAdjustLogMapper = adminPointAdjustLogMapper;
    }

    /**
     * 查询指定用户的积分账户（管理员视角）.
     *
     * @param targetUserId 被查询用户 ID
     * @return 积分账户，若未开户则返回空账户 VO（余额为 0）
     */
    public PointAccount getAccountByUserId(Long targetUserId) {
        return pointAccountRepository.findByUserId(targetUserId)
                .orElseGet(() -> PointAccount.create(targetUserId));
    }

    /**
     * 管理员调整积分（正负均可）.
     *
     * @param targetUserId     被调分用户 ID
     * @param delta            调整量（正=增加，负=减少，不能为 0）
     * @param reason           调整原因（必填）
     * @param bizNo            幂等单号
     * @param operatorUserId   管理员用户 ID
     * @param operatorUsername 管理员用户名
     * @return 调整后的积分余额
     * @throws GrowthBusinessException 参数非法或余额不足时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public int adjustPoints(Long targetUserId, int delta, String reason,
                            String bizNo, Long operatorUserId, String operatorUsername) {
        // ① 参数校验
        if (delta == 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "调整量不能为 0");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "调整原因不能为空");
        }

        // ② 查询或初始化积分账户
        PointAccount account = pointAccountRepository.findByUserId(targetUserId)
                .orElseGet(() -> {
                    PointAccount newAccount = PointAccount.create(targetUserId);
                    pointAccountRepository.save(newAccount);
                    return pointAccountRepository.findByUserId(targetUserId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "积分账户初始化失败，userId=" + targetUserId));
                });

        // ③ 扣分时校验余额
        if (delta < 0 && !account.canDebit(-delta)) {
            throw new GrowthBusinessException(GrowthErrorCode.INSUFFICIENT_POINTS,
                    "扣分后余额将为负数，当前余额=" + account.getBalance() + "，扣减量=" + (-delta));
        }

        // ④ 调用聚合根执行调整
        account.adjust(delta);

        // ⑤ CAS 乐观锁更新账户
        account = updateAccountWithRetry(account, targetUserId, delta);

        // ⑥ INSERT IGNORE 写入积分流水（幂等）
        PointJournal journal = PointJournal.create(
                targetUserId, delta, account.getBalance(),
                SOURCE_ADMIN_ADJUST, bizNo,
                "管理员调整：" + reason, operatorUsername);
        try {
            pointJournalRepository.insertIgnore(journal);
        } catch (DuplicateKeyException e) {
            log.info("[管理员调分] 幂等跳过，bizNo={}", bizNo);
            // 幂等重复时回滚并返回当前余额（不做双重处理）
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID,
                    "重复的 bizNo，调整已处理过：" + bizNo);
        }

        // ⑦ 写入审计日志
        AdminPointAdjustLogDO adjustLog = new AdminPointAdjustLogDO();
        adjustLog.setTargetUserId(targetUserId);
        adjustLog.setDelta(delta);
        adjustLog.setReason(reason);
        adjustLog.setBizNo(bizNo);
        adjustLog.setOperatorUserId(operatorUserId);
        adjustLog.setOperatorUsername(operatorUsername);
        adjustLog.setCreatedAt(LocalDateTime.now());
        adjustLog.setVersion(0);
        adminPointAdjustLogMapper.insert(adjustLog);

        log.info("[管理员调分] 成功。targetUserId={}, delta={}, balanceAfter={}, operator={}",
                targetUserId, delta, account.getBalance(), operatorUsername);
        return account.getBalance();
    }

    // ────────────────────────── 私有辅助方法 ──────────────────────────────

    /**
     * 带乐观锁重试的账户更新.
     *
     * @param account  已执行 adjust() 的账户
     * @param userId   用户 ID（重试时重新加载）
     * @param delta    调整量（重试时重新应用）
     * @return 更新后最新的账户
     */
    private PointAccount updateAccountWithRetry(PointAccount account, Long userId, int delta) {
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows = pointAccountRepository.updateCAS(account);
            if (rows > 0) {
                return account;
            }
            // 版本冲突：重新加载并重新执行业务方法
            account = pointAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("积分账户不存在，userId=" + userId));
            if (delta < 0 && !account.canDebit(-delta)) {
                throw new GrowthBusinessException(GrowthErrorCode.INSUFFICIENT_POINTS,
                        "扣分后余额将为负数，当前余额=" + account.getBalance() + "，扣减量=" + (-delta));
            }
            account.adjust(delta);
        }
        throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "积分调整并发冲突，请稍后重试");
    }
}

