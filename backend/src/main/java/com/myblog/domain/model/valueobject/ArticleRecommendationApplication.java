package com.myblog.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 首页推荐申请值对象.
 *
 * @author Codex
 * @since 2026-05-17
 */
public class ArticleRecommendationApplication {

    private Long id;
    private Long articleId;
    private Long authorId;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private String reviewNote;
    private int version;

    /**
     * 创建新的推荐申请.
     *
     * @param articleId 文章ID
     * @param authorId  作者ID
     * @return 推荐申请
     */
    public static ArticleRecommendationApplication create(Long articleId, Long authorId) {
        ArticleRecommendationApplication application = new ArticleRecommendationApplication();
        application.articleId = articleId;
        application.authorId = authorId;
        application.status = "PENDING";
        application.appliedAt = LocalDateTime.now();
        return application;
    }

    /**
     * 审批通过.
     *
     * @param reviewerId 审核人
     * @param reviewNote 备注
     */
    public void approve(Long reviewerId, String reviewNote) {
        this.status = "APPROVED";
        this.reviewedBy = reviewerId;
        this.reviewNote = reviewNote;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 审批拒绝.
     *
     * @param reviewerId 审核人
     * @param reviewNote 备注
     */
    public void reject(Long reviewerId, String reviewNote) {
        this.status = "REJECTED";
        this.reviewedBy = reviewerId;
        this.reviewNote = reviewNote;
        this.reviewedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
