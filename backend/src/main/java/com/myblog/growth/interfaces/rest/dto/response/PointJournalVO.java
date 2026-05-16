package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDateTime;

/**
 * 积分流水视图对象（只读展示）.
 * <p>对应 GET /api/points/journals 接口列表项。</p>
 */
public class PointJournalVO {

    /** 流水 ID. */
    private Long id;

    /** 积分变化量（正=入账，负=扣减）. */
    private int delta;

    /** 变更后余额快照. */
    private int balanceAfter;

    /** 来源类型（SIGN_IN / INVITE / RECHARGE / UNLOCK / ADMIN_ADJUST 等）. */
    private String sourceType;

    /** 业务单号. */
    private String bizNo;

    /** 备注. */
    private String remark;

    /** 关联文章 ID（文章解锁/分成流水展示用，可为 null）. */
    private Long articleId;

    /** 关联文章标题（可为 null）. */
    private String articleTitle;

    /** 关联文章状态（可为 null）. */
    private String articleStatus;

    /** 关联文章是否可点击访问. */
    private boolean articleAccessible;

    /** 创建时间. */
    private LocalDateTime createdAt;

    /** 默认构造. */
    public PointJournalVO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }

    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public String getArticleTitle() { return articleTitle; }
    public void setArticleTitle(String articleTitle) { this.articleTitle = articleTitle; }

    public String getArticleStatus() { return articleStatus; }
    public void setArticleStatus(String articleStatus) { this.articleStatus = articleStatus; }

    public boolean isArticleAccessible() { return articleAccessible; }
    public void setArticleAccessible(boolean articleAccessible) { this.articleAccessible = articleAccessible; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

