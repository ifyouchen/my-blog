package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.aggregate.PointAccount;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.domain.repository.PointAccountRepository;
import com.myblog.growth.domain.repository.PointJournalRepository;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 积分账户应用服务.
 *
 * <p>
 * 负责积分入账、扣减，处理乐观锁重试与幂等控制。
 * 所有写操作必须在同一事务内完成，确保：账户余额更新 + 流水写入 原子提交。
 * </p>
 *
 * <pre>
 * 核心流程（入账/扣减）：
 *   ① 查询或初始化积分账户
 *   ② 调用聚合根业务方法（credit / debit）
 *   ③ 乐观锁 CAS 更新账户（最多重试 3 次）
 *   ④ INSERT IGNORE 写入积分流水（幂等保证）
 * </pre>
 */
@Service
public class PointAppService {

    private static final Logger log = LoggerFactory.getLogger(PointAppService.class);

    /** 乐观锁冲突时的最大重试次数. */
    private static final int MAX_RETRY = 3;

    private final PointAccountRepository pointAccountRepository;
    private final PointJournalRepository pointJournalRepository;

    /**
     * 构造注入依赖.
     *
     * @param pointAccountRepository 积分账户 Repository
     * @param pointJournalRepository 积分流水 Repository
     */
    public PointAppService(PointAccountRepository pointAccountRepository,
                           PointJournalRepository pointJournalRepository) {
        this.pointAccountRepository = pointAccountRepository;
        this.pointJournalRepository = pointJournalRepository;
    }

    /**
     * 积分入账（正数变更）.
     * <p>
     * 幂等：相同 bizNo 重复调用只写一条流水，余额只增一次。
     * </p>
     *
     * @param userId     目标用户 ID
     * @param delta      入账积分（必须 &gt; 0）
     * @param sourceType 来源类型（如 SIGN_IN / INVITE / RECHARGE）
     * @param bizNo      业务单号（幂等键）
     * @param remark     备注（可为 null）
     * @param operator   操作人（可为 null）
     * @return 入账后的积分余额
     */
    @Transactional(rollbackFor = Exception.class)
    public int addPoints(Long userId, int delta, String sourceType,
                         String bizNo, String remark, String operator) {
        if (delta <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "入账积分必须大于 0，实际值：" + delta);
        }

        // 查询或初始化账户
        PointAccount account = getOrCreateAccount(userId);

        // 执行入账
        account.credit(delta);

        // CAS 更新账户（乐观锁重试）
        account = updateAccountWithRetry(account, userId, delta, false);

        // 写入流水（INSERT IGNORE，幂等）
        PointJournal journal = PointJournal.create(
                userId, delta, account.getBalance(), sourceType, bizNo, remark, operator);
        try {
            pointJournalRepository.insertIgnore(journal);
        } catch (DuplicateKeyException e) {
            log.info("[积分入账] 幂等跳过，bizNo={}", bizNo);
        }

        log.info("[积分入账] 成功。userId={}, sourceType={}, delta={}, balance={}", userId, sourceType, delta, account.getBalance());
        return account.getBalance();
    }

    /**
     * 积分扣减（负数变更）.
     *
     * @param userId     目标用户 ID
     * @param delta      扣减积分（必须 &gt; 0）
     * @param sourceType 来源类型（如 UNLOCK）
     * @param bizNo      业务单号（幂等键）
     * @param remark     备注（可为 null）
     * @param operator   操作人（可为 null）
     * @return 扣减后的积分余额
     * @throws GrowthBusinessException 余额不足时抛出 INSUFFICIENT_POINTS
     */
    @Transactional(rollbackFor = Exception.class)
    public int deductPoints(Long userId, int delta, String sourceType,
                            String bizNo, String remark, String operator) {
        if (delta <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "扣减积分必须大于 0，实际值：" + delta);
        }

        // 查询账户（不存在则余额为 0，必然不足）
        PointAccount account = pointAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new GrowthBusinessException(GrowthErrorCode.INSUFFICIENT_POINTS, "积分账户不存在"));

        if (!account.canDebit(delta)) {
            throw new GrowthBusinessException(GrowthErrorCode.INSUFFICIENT_POINTS,
                    "积分余额不足，余额=" + account.getBalance() + "，需要=" + delta);
        }

        // 执行扣减
        account.debit(delta);

        // CAS 更新账户（乐观锁重试）
        account = updateAccountWithRetry(account, userId, delta, true);

        // 写入流水（INSERT IGNORE，幂等）
        PointJournal journal = PointJournal.create(
                userId, -delta, account.getBalance(), sourceType, bizNo, remark, operator);
        try {
            pointJournalRepository.insertIgnore(journal);
        } catch (DuplicateKeyException e) {
            log.info("[积分扣减] 幂等跳过，bizNo={}", bizNo);
        }

        log.info("[积分扣减] 成功。userId={}, sourceType={}, delta={}, balance={}", userId, sourceType, delta, account.getBalance());
        return account.getBalance();
    }

    /**
     * 查询用户积分账户余额及统计.
     *
     * @param userId 用户 ID
     * @return 积分账户（不存在则返回余额为 0 的虚拟账户）
     */
    public PointAccount getAccount(Long userId) {
        return pointAccountRepository.findByUserId(userId)
                .orElse(PointAccount.create(userId));
    }

    /**
     * 分页查询积分流水.
     *
     * @param userId     用户 ID
     * @param sourceType 来源类型筛选（null 表示全部）
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @return 积分流水列表
     */
    public List<PointJournal> getJournals(Long userId, String sourceType, int page, int size) {
        return pointJournalRepository.findPageByUserId(userId, sourceType, page, size);
    }

    /**
     * 统计积分流水总数.
     *
     * @param userId     用户 ID
     * @param sourceType 来源类型筛选（null 表示全部）
     * @return 总数
     */
    public long countJournals(Long userId, String sourceType) {
        return pointJournalRepository.countByUserId(userId, sourceType);
    }

    // ────────────────────────── 私有辅助方法 ──────────────────────────────

    /**
     * 查询或初始化积分账户.
     *
     * @param userId 用户 ID
     * @return 积分账户
     */
    private PointAccount getOrCreateAccount(Long userId) {
        return pointAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointAccount newAccount = PointAccount.create(userId);
                    pointAccountRepository.save(newAccount);
                    return pointAccountRepository.findByUserId(userId)
                            .orElseThrow(() -> new IllegalStateException("积分账户初始化失败，userId=" + userId));
                });
    }

    /**
     * 带乐观锁重试的账户更新.
     *
     * @param account 已执行业务方法的账户
     * @param userId  用户 ID（用于重试时重新加载）
     * @param delta   变化量（用于重试时重新应用）
     * @param debit   true=扣减，false=入账
     * @return 更新后最新的账户
     */
    private PointAccount updateAccountWithRetry(PointAccount account, Long userId, int delta, boolean debit) {
        for (int i = 0; i < MAX_RETRY; i++) {
            int rows = pointAccountRepository.updateCAS(account);
            if (rows > 0) {
                return account;
            }
            // 版本冲突：重新加载并重新执行业务方法
            account = pointAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("积分账户不存在，userId=" + userId));
            if (debit) {
                if (!account.canDebit(delta)) {
                    throw new GrowthBusinessException(GrowthErrorCode.INSUFFICIENT_POINTS,
                            "积分余额不足，余额=" + account.getBalance() + "，需要=" + delta);
                }
                account.debit(delta);
            } else {
                account.credit(delta);
            }
        }
        throw new GrowthBusinessException(GrowthErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "积分更新并发冲突，请稍后重试");
    }
}

