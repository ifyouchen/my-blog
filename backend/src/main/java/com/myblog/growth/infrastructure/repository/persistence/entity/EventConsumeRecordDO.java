package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 事件幂等消费记录数据库实体（MyBatis DO）.
 * <p>
 * 对应数据库表 {@code event_consume_record}。
 * 通过唯一索引 {@code uk_event_consumer(event_id, consumer_name)} 保证幂等消费。
 * </p>
 */
public class EventConsumeRecordDO {

    /** 主键 ID（自增）. */
    private Long id;

    /** 事件唯一 ID（幂等键基础）. */
    private String eventId;

    /** 消费者名称（通常为处理器类名）. */
    private String consumerName;

    /** 消费状态：PROCESSING / SUCCESS / FAILED. */
    private String status;

    /** 失败原因（仅状态为 FAILED 时有值）. */
    private String errorMsg;

    /** 创建时间. */
    private LocalDateTime createdAt;

    /** 最后更新时间. */
    private LocalDateTime updatedAt;

    /** 软删除时间（null 表示未删除）. */
    private LocalDateTime deletedAt;

    /** 乐观锁版本号. */
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}

