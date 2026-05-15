package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 经验流水值对象（不可变审计记录）.
 * <p>
 * 对应数据库 {@code user_exp_journal} 表。
 * 流水记录只允许 INSERT，禁止 UPDATE 和物理 DELETE，是经验变更的完整审计链路。
 * 通过 {@code idempotentKey} 唯一索引防止重复入账。
 * </p>
 */
public class ExpJournal {

    /** 数据库主键 ID. */
    private Long id;

    /** 用户 ID. */
    private Long userId;

    /** 经验变化量（正数 = 增加，负数保留用于冲正）. */
    private int delta;

    /** 变更后的余额快照（便于对账，无需累加历史流水）. */
    private int balanceAfter;

    /** 行为类型（如 LIKE / READ / PUBLISH / LEVEL_UP）. */
    private String eventType;

    /** 来源 ID（文章 ID / 评论 ID 等，可为 null）. */
    private Long sourceId;

    /** 备注信息（可为 null）. */
    private String remark;

    /**
     * 幂等键，格式为 {@code {eventType}:{role}:{userId}:{sourceId}:{eventId}}.
     * 由唯一索引 {@code uk_idempotent_key} 保证全局唯一。
     */
    private String idempotentKey;

    /** 流水创建时间. */
    private LocalDateTime createdAt;

    /** 乐观锁版本号. */
    private int version;

    /** 禁止外部直接构造，使用工厂方法. */
    private ExpJournal() {
    }

    /**
     * 工厂方法：创建新的经验流水记录（未持久化）.
     *
     * @param userId        用户 ID
     * @param delta         经验变化量
     * @param balanceAfter  变更后余额快照
     * @param eventType     行为类型
     * @param sourceId      来源 ID（可为 null）
     * @param remark        备注（可为 null）
     * @param idempotentKey 幂等键
     * @return 新的经验流水值对象
     */
    public static ExpJournal create(Long userId, int delta, int balanceAfter,
                                    String eventType, Long sourceId, String remark, String idempotentKey) {
        ExpJournal journal = new ExpJournal();
        journal.userId = userId;
        journal.delta = delta;
        journal.balanceAfter = balanceAfter;
        journal.eventType = eventType;
        journal.sourceId = sourceId;
        journal.remark = remark;
        journal.idempotentKey = idempotentKey;
        journal.createdAt = LocalDateTime.now();
        journal.version = 0;
        return journal;
    }

    /**
     * 工厂方法：从持久化数据重建经验流水.
     *
     * @param id            主键 ID
     * @param userId        用户 ID
     * @param delta         经验变化量
     * @param balanceAfter  变更后余额快照
     * @param eventType     行为类型
     * @param sourceId      来源 ID
     * @param remark        备注
     * @param idempotentKey 幂等键
     * @param createdAt     创建时间
     * @param version       乐观锁版本号
     * @return 重建后的经验流水值对象
     */
    public static ExpJournal restore(Long id, Long userId, int delta, int balanceAfter,
                                     String eventType, Long sourceId, String remark,
                                     String idempotentKey, LocalDateTime createdAt, int version) {
        ExpJournal journal = new ExpJournal();
        journal.id = id;
        journal.userId = userId;
        journal.delta = delta;
        journal.balanceAfter = balanceAfter;
        journal.eventType = eventType;
        journal.sourceId = sourceId;
        journal.remark = remark;
        journal.idempotentKey = idempotentKey;
        journal.createdAt = createdAt;
        journal.version = version;
        return journal;
    }

    /**
     * 获取主键 ID.
     *
     * @return 主键 ID（未持久化时为 null）
     */
    public Long getId() { return id; }

    /**
     * 获取用户 ID.
     *
     * @return 用户 ID
     */
    public Long getUserId() { return userId; }

    /**
     * 获取经验变化量.
     *
     * @return 变化量（正数为增加）
     */
    public int getDelta() { return delta; }

    /**
     * 获取变更后的余额快照.
     *
     * @return 变更后经验余额
     */
    public int getBalanceAfter() { return balanceAfter; }

    /**
     * 获取行为类型.
     *
     * @return 行为类型字符串
     */
    public String getEventType() { return eventType; }

    /**
     * 获取来源 ID.
     *
     * @return 来源 ID（可为 null）
     */
    public Long getSourceId() { return sourceId; }

    /**
     * 获取备注.
     *
     * @return 备注（可为 null）
     */
    public String getRemark() { return remark; }

    /**
     * 获取幂等键.
     *
     * @return 幂等键字符串
     */
    public String getIdempotentKey() { return idempotentKey; }

    /**
     * 获取流水创建时间.
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * 获取乐观锁版本号.
     *
     * @return 版本号
     */
    public int getVersion() { return version; }
}

