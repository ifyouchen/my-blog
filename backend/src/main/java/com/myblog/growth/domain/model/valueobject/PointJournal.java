package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 积分流水值对象（不可变审计记录）.
 * <p>
 * 对应数据库 {@code user_point_journal} 表。
 * 流水只允许 INSERT，禁止 UPDATE 和物理 DELETE。
 * 通过 {@code bizNo} 唯一索引（uk_biz_no）防止重复入账/扣款。
 * </p>
 */
public class PointJournal {

    /** 数据库主键 ID. */
    private Long id;

    /** 用户 ID. */
    private Long userId;

    /**
     * 积分变化量（正数=收入，负数=支出）.
     * <p>如：签到 +10、解锁文章 -50。</p>
     */
    private int delta;

    /** 变更后的积分余额快照（用于对账）. */
    private int balanceAfter;

    /**
     * 来源类型（对应 PointEventType 枚举值），对应数据库字段 {@code source_type}.
     * <p>如：SIGN_IN / INVITE / RECHARGE / UNLOCK / ADMIN_ADJUST / PUBLISH</p>
     */
    private String sourceType;

    /**
     * 业务单号，全局幂等键，对应数据库字段 {@code biz_no}.
     * <p>格式建议：{@code {sourceType}:{userId}:{bizId}}</p>
     */
    private String bizNo;

    /** 备注（如"管理员调整：补偿奖励"，可为 null）. */
    private String remark;

    /** 操作人（管理员调分时填写，可为 null）. */
    private String operator;

    /** 流水创建时间. */
    private LocalDateTime createdAt;

    /** 乐观锁版本号. */
    private int version;

    /** 禁止外部直接构造，使用工厂方法. */
    private PointJournal() {
    }

    /**
     * 工厂方法：创建新的积分流水记录（未持久化）.
     *
     * @param userId       用户 ID
     * @param delta        积分变化量
     * @param balanceAfter 变更后余额快照
     * @param sourceType   来源类型
     * @param bizNo        业务单号（幂等键）
     * @param remark       备注（可为 null）
     * @param operator     操作人（可为 null）
     * @return 新的积分流水值对象
     */
    public static PointJournal create(Long userId, int delta, int balanceAfter,
                                      String sourceType, String bizNo,
                                      String remark, String operator) {
        PointJournal journal = new PointJournal();
        journal.userId = userId;
        journal.delta = delta;
        journal.balanceAfter = balanceAfter;
        journal.sourceType = sourceType;
        journal.bizNo = bizNo;
        journal.remark = remark;
        journal.operator = operator;
        journal.createdAt = LocalDateTime.now();
        journal.version = 0;
        return journal;
    }

    /**
     * 工厂方法：从持久化数据重建积分流水.
     *
     * @param id           主键 ID
     * @param userId       用户 ID
     * @param delta        积分变化量
     * @param balanceAfter 变更后余额快照
     * @param sourceType   来源类型
     * @param bizNo        业务单号
     * @param remark       备注
     * @param operator     操作人
     * @param createdAt    创建时间
     * @param version      乐观锁版本号
     * @return 重建后的积分流水值对象
     */
    public static PointJournal restore(Long id, Long userId, int delta, int balanceAfter,
                                       String sourceType, String bizNo,
                                       String remark, String operator,
                                       LocalDateTime createdAt, int version) {
        PointJournal journal = new PointJournal();
        journal.id = id;
        journal.userId = userId;
        journal.delta = delta;
        journal.balanceAfter = balanceAfter;
        journal.sourceType = sourceType;
        journal.bizNo = bizNo;
        journal.remark = remark;
        journal.operator = operator;
        journal.createdAt = createdAt;
        journal.version = version;
        return journal;
    }

    /** 获取主键 ID. */
    public Long getId() { return id; }
    /** 获取用户 ID. */
    public Long getUserId() { return userId; }
    /** 获取积分变化量. */
    public int getDelta() { return delta; }
    /** 获取变更后余额快照. */
    public int getBalanceAfter() { return balanceAfter; }
    /** 获取来源类型. */
    public String getSourceType() { return sourceType; }
    /** 获取业务单号（幂等键）. */
    public String getBizNo() { return bizNo; }
    /** 获取备注. */
    public String getRemark() { return remark; }
    /** 获取操作人. */
    public String getOperator() { return operator; }
    /** 获取创建时间. */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** 获取版本号. */
    public int getVersion() { return version; }
}

