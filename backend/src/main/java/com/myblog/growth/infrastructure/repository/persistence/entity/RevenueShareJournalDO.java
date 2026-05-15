package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 分账流水数据库实体.
 * <p>对应 {@code revenue_share_journal} 表。</p>
 */
public class RevenueShareJournalDO {

    private Long id;
    private String orderNo;
    private Long articleId;
    private Long authorId;
    private int totalPoints;
    private int platformPoints;
    private int authorPoints;
    private String shareRatio;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getPlatformPoints() { return platformPoints; }
    public void setPlatformPoints(int platformPoints) { this.platformPoints = platformPoints; }

    public int getAuthorPoints() { return authorPoints; }
    public void setAuthorPoints(int authorPoints) { this.authorPoints = authorPoints; }

    public String getShareRatio() { return shareRatio; }
    public void setShareRatio(String shareRatio) { this.shareRatio = shareRatio; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}

