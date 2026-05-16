package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 分账流水只读视图.
 */
public class RevenueShareJournal {

    private final Long id;
    private final String orderNo;
    private final Long articleId;
    private final Long authorId;
    private final int totalPoints;
    private final int platformPoints;
    private final int authorPoints;
    private final String shareRatio;
    private final String settlementStatus;
    private final String pointJournalBizNo;
    private final LocalDateTime settledAt;
    private final int retryCount;
    private final String lastError;
    private final LocalDateTime createdAt;

    private RevenueShareJournal(Builder builder) {
        this.id = builder.id;
        this.orderNo = builder.orderNo;
        this.articleId = builder.articleId;
        this.authorId = builder.authorId;
        this.totalPoints = builder.totalPoints;
        this.platformPoints = builder.platformPoints;
        this.authorPoints = builder.authorPoints;
        this.shareRatio = builder.shareRatio;
        this.settlementStatus = builder.settlementStatus;
        this.pointJournalBizNo = builder.pointJournalBizNo;
        this.settledAt = builder.settledAt;
        this.retryCount = builder.retryCount;
        this.lastError = builder.lastError;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Long getArticleId() { return articleId; }
    public Long getAuthorId() { return authorId; }
    public int getTotalPoints() { return totalPoints; }
    public int getPlatformPoints() { return platformPoints; }
    public int getAuthorPoints() { return authorPoints; }
    public String getShareRatio() { return shareRatio; }
    public String getSettlementStatus() { return settlementStatus; }
    public String getPointJournalBizNo() { return pointJournalBizNo; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public int getRetryCount() { return retryCount; }
    public String getLastError() { return lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class Builder {
        private Long id;
        private String orderNo;
        private Long articleId;
        private Long authorId;
        private int totalPoints;
        private int platformPoints;
        private int authorPoints;
        private String shareRatio;
        private String settlementStatus;
        private String pointJournalBizNo;
        private LocalDateTime settledAt;
        private int retryCount;
        private String lastError;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder orderNo(String orderNo) { this.orderNo = orderNo; return this; }
        public Builder articleId(Long articleId) { this.articleId = articleId; return this; }
        public Builder authorId(Long authorId) { this.authorId = authorId; return this; }
        public Builder totalPoints(int totalPoints) { this.totalPoints = totalPoints; return this; }
        public Builder platformPoints(int platformPoints) { this.platformPoints = platformPoints; return this; }
        public Builder authorPoints(int authorPoints) { this.authorPoints = authorPoints; return this; }
        public Builder shareRatio(String shareRatio) { this.shareRatio = shareRatio; return this; }
        public Builder settlementStatus(String settlementStatus) {
            this.settlementStatus = settlementStatus;
            return this;
        }
        public Builder pointJournalBizNo(String pointJournalBizNo) {
            this.pointJournalBizNo = pointJournalBizNo;
            return this;
        }
        public Builder settledAt(LocalDateTime settledAt) { this.settledAt = settledAt; return this; }
        public Builder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        public Builder lastError(String lastError) { this.lastError = lastError; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RevenueShareJournal build() { return new RevenueShareJournal(this); }
    }
}
