package com.myblog.growth.application.event;

import java.time.LocalDateTime;

/**
 * 行为经验领域事件 DTO.
 * <p>
 * 由 {@code GrantExpOnBehaviorEventHandler} 将博客侧原始事件适配为此统一格式，
 * 再交给 {@code ExpGrantAppService} 处理。不可变，构建后不可修改。
 * </p>
 */
public class BehaviorExpEvent {

    /**
     * 全局唯一事件 ID，用于幂等控制.
     * <p>格式建议：{@code evt-{eventType}-{actorUserId}-{sourceId}-{timestamp}}</p>
     */
    private final String eventId;

    /** 行为类型（如 LIKE / READ / PUBLISH），对应 GrowthEventType 枚举值. */
    private final String eventType;

    /** 操作者用户 ID（执行行为的用户）. */
    private final Long actorUserId;

    /**
     * 内容作者用户 ID（被操作内容的作者，可为 null）.
     * <p>PUBLISH 行为时 authorUserId 与 actorUserId 相同，传 null 即可；
     * 事件发放逻辑会跳过 AUTHOR 角色。</p>
     */
    private final Long authorUserId;

    /** 来源 ID（文章 ID / 评论 ID 等，可为 null）. */
    private final Long sourceId;

    /** 事件发生时间. */
    private final LocalDateTime occurredAt;

    /**
     * 全参构造，推荐使用 {@link #builder()} 构建。
     *
     * @param eventId      事件 ID
     * @param eventType    行为类型
     * @param actorUserId  操作者用户 ID
     * @param authorUserId 内容作者用户 ID（可为 null）
     * @param sourceId     来源 ID（可为 null）
     * @param occurredAt   事件发生时间
     */
    private BehaviorExpEvent(String eventId, String eventType, Long actorUserId,
                              Long authorUserId, Long sourceId, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.authorUserId = authorUserId;
        this.sourceId = sourceId;
        this.occurredAt = occurredAt;
    }

    /**
     * 创建 Builder.
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * BehaviorExpEvent Builder.
     */
    public static class Builder {
        private String eventId;
        private String eventType;
        private Long actorUserId;
        private Long authorUserId;
        private Long sourceId;
        private LocalDateTime occurredAt;

        /** 设置事件 ID. */
        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        /** 设置行为类型. */
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        /** 设置操作者用户 ID. */
        public Builder actorUserId(Long actorUserId) { this.actorUserId = actorUserId; return this; }
        /** 设置内容作者用户 ID. */
        public Builder authorUserId(Long authorUserId) { this.authorUserId = authorUserId; return this; }
        /** 设置来源 ID. */
        public Builder sourceId(Long sourceId) { this.sourceId = sourceId; return this; }
        /** 设置事件发生时间. */
        public Builder occurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; return this; }

        /**
         * 构建事件对象.
         *
         * @return 不可变的 BehaviorExpEvent
         */
        public BehaviorExpEvent build() {
            return new BehaviorExpEvent(eventId, eventType, actorUserId, authorUserId, sourceId, occurredAt);
        }
    }

    /** 获取事件 ID. */
    public String getEventId() { return eventId; }

    /** 获取行为类型. */
    public String getEventType() { return eventType; }

    /** 获取操作者用户 ID. */
    public Long getActorUserId() { return actorUserId; }

    /** 获取内容作者用户 ID（可为 null）. */
    public Long getAuthorUserId() { return authorUserId; }

    /** 获取来源 ID（可为 null）. */
    public Long getSourceId() { return sourceId; }

    /** 获取事件发生时间. */
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

