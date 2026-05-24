package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 首页推荐申请提交事件.
 * <p>
 * 当创作者提交首页推荐申请时触发，用于通知管理员审核.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class RecommendationAppliedEvent {

    /** 申请记录ID. */
    private final Long applicationId;

    /** 申请人用户ID. */
    private final Long applicantUserId;

    /** 申请推荐文章ID. */
    private final Long articleId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造首页推荐申请提交事件.
     *
     * @param applicationId  申请记录ID
     * @param applicantUserId 申请人用户ID
     * @param articleId      申请推荐文章ID
     */
    public RecommendationAppliedEvent(Long applicationId, Long applicantUserId, Long articleId) {
        this.applicationId = applicationId;
        this.applicantUserId = applicantUserId;
        this.articleId = articleId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getApplicantUserId() {
        return applicantUserId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
