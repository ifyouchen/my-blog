package com.myblog.growth.domain.model.aggregate;

import java.time.LocalDateTime;

/**
 * 文章解锁订单聚合根.
 *
 * <p>
 * 对应数据库 {@code article_unlock_order} 表。
 * 管理解锁订单从 PENDING → SUCCESS / FAILED 的状态流转，
 * 使用工厂方法创建和恢复，禁止外部直接 new。
 * </p>
 *
 * <p>状态流：</p>
 * <pre>
 *   create()  → PENDING
 *   complete()  → SUCCESS
 *   fail(reason) → FAILED
 * </pre>
 */
public class UnlockOrder {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED  = "FAILED";

    /** 数据库主键 ID. */
    private Long id;

    /** 解锁订单号（幂等键）. */
    private String orderNo;

    /** 解锁用户 ID. */
    private Long userId;

    /** 文章 ID. */
    private Long articleId;

    /** 扣除积分数. */
    private int pointsCost;

    /** 订单状态（PENDING / SUCCESS / FAILED）. */
    private String status;

    /** 失败原因（仅 FAILED 时有值）. */
    private String failReason;

    /** 创建时间. */
    private LocalDateTime createdAt;

    /** 更新时间. */
    private LocalDateTime updatedAt;

    /** 乐观锁版本号. */
    private int version;

    /** 禁止外部直接构造，使用工厂方法. */
    private UnlockOrder() {
    }

    // ────────────────────────── 工厂方法 ──────────────────────────────────

    /**
     * 创建新解锁订单（初始状态 PENDING）.
     *
     * @param orderNo    订单号
     * @param userId     用户 ID
     * @param articleId  文章 ID
     * @param pointsCost 扣除积分数
     * @return 新建的解锁订单
     */
    public static UnlockOrder create(String orderNo, Long userId, Long articleId, int pointsCost) {
        UnlockOrder o = new UnlockOrder();
        o.orderNo    = orderNo;
        o.userId     = userId;
        o.articleId  = articleId;
        o.pointsCost = pointsCost;
        o.status     = STATUS_PENDING;
        o.createdAt  = LocalDateTime.now();
        o.updatedAt  = LocalDateTime.now();
        o.version    = 0;
        return o;
    }

    /**
     * 从数据库恢复解锁订单（供 Repository 使用）.
     *
     * @param id          主键 ID
     * @param orderNo     订单号
     * @param userId      用户 ID
     * @param articleId   文章 ID
     * @param pointsCost  扣除积分数
     * @param status      状态
     * @param failReason  失败原因
     * @param createdAt   创建时间
     * @param updatedAt   更新时间
     * @param version     乐观锁版本
     * @return 恢复的解锁订单
     */
    public static UnlockOrder restore(Long id, String orderNo, Long userId, Long articleId,
                                      int pointsCost, String status, String failReason,
                                      LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        UnlockOrder o = new UnlockOrder();
        o.id         = id;
        o.orderNo    = orderNo;
        o.userId     = userId;
        o.articleId  = articleId;
        o.pointsCost = pointsCost;
        o.status     = status;
        o.failReason = failReason;
        o.createdAt  = createdAt;
        o.updatedAt  = updatedAt;
        o.version    = version;
        return o;
    }

    // ────────────────────────── 业务方法 ──────────────────────────────────

    /**
     * 标记订单成功.
     *
     * @throws IllegalStateException 非 PENDING 状态时抛出
     */
    public void complete() {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException("只有 PENDING 状态的订单才能完成，当前状态=" + this.status);
        }
        this.status    = STATUS_SUCCESS;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记订单失败.
     *
     * @param reason 失败原因
     * @throws IllegalStateException 非 PENDING 状态时抛出
     */
    public void fail(String reason) {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException("只有 PENDING 状态的订单才能失败，当前状态=" + this.status);
        }
        this.status    = STATUS_FAILED;
        this.failReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断订单是否已完成（SUCCESS）.
     *
     * @return {@code true} 表示已成功
     */
    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(this.status);
    }

    /**
     * 判断订单是否处于待处理状态.
     *
     * @return {@code true} 表示 PENDING
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(this.status);
    }

    // ────────────────────────── 主键回填方法 ─────────────────────────────

    /**
     * 回填数据库自增主键（供 Repository 调用）.
     *
     * @param id 主键 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    // ────────────────────────── Getter ────────────────────────────────────

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Long getUserId() { return userId; }
    public Long getArticleId() { return articleId; }
    public int getPointsCost() { return pointsCost; }
    public String getStatus() { return status; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
}

