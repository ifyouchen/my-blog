package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 文章解锁关系数据库实体（解锁凭证）.
 * <p>对应 {@code article_unlock_relation} 表。</p>
 */
public class ArticleUnlockRelationDO {

    private Long id;
    private Long userId;
    private Long articleId;
    private String orderNo;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}

